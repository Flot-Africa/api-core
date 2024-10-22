package africa.flot.infrastructure.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import africa.flot.infrastructure.minio.MinioService;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class DanayaService {
    private static final Logger LOG = Logger.getLogger(DanayaService.class);

    private final WebClient webClient;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.url")
    String baseUrl;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.api-secret")
    String apiSecret;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.polling-interval-seconds", defaultValue = "5")
    int pollingIntervalSeconds;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.initial-delay-seconds", defaultValue = "3")
    int initialDelaySeconds;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.max-polling-attempts", defaultValue = "60")
    int maxPollingAttempts;

    private static final List<String> DEFAULT_VERIFICATIONS = Arrays.asList(
            "DB_CHECK",
            "EXPIRATION_CHECK",
            "TEMPLATE_CHECK"
    );

    @Inject
    MinioService minioService;

    @Inject
    public DanayaService(Vertx vertx) {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout(5000)
                .setIdleTimeout(10000);
        this.webClient = WebClient.create(vertx, options);
    }

    public Uni<DanayaVerificationResult> verifyIdDocumentWithPolling(String bucketName, String frontImageName, String backImageName) {
        LOG.infof("Démarrage de la vérification des documents avec polling - bucket: %s, front: %s, back: %s",
                bucketName, frontImageName, backImageName);

        return verifyIdDocument(bucketName, frontImageName, backImageName)
                .flatMap(initialResponse -> {
                    String verificationUuid = initialResponse.getString("id");
                    LOG.infof("Documents uploadés avec succès. UUID de vérification: %s", verificationUuid);
                    // Ajout du délai initial avant de commencer le polling
                    return Uni.createFrom().nullItem()
                            .onItem().delayIt().by(Duration.ofSeconds(initialDelaySeconds))
                            .flatMap(ignored -> pollVerificationStatus(verificationUuid, 0));
                });
    }

    private Uni<DanayaVerificationResult> pollVerificationStatus(String verificationUuid, int attemptCount) {
        if (attemptCount >= maxPollingAttempts) {
            LOG.errorf("Délai d'attente dépassé pour la vérification %s après %d tentatives",
                    verificationUuid, attemptCount);
            return Uni.createFrom().failure(
                    new RuntimeException("Délai d'attente dépassé pour la vérification des documents")
            );
        }

        return checkVerificationStatus(verificationUuid)
                .onFailure().transform(error -> {
                    if (error.getMessage().contains("404")) {
                        LOG.infof("Document en cours d'initialisation, nouvelle tentative dans %d secondes",
                                pollingIntervalSeconds);
                        return new DocumentNotReadyException();
                    }
                    return error;
                })
                .onFailure(DocumentNotReadyException.class).recoverWithUni(() ->
                        Uni.createFrom().nullItem()
                                .onItem().delayIt().by(Duration.ofSeconds(pollingIntervalSeconds))
                                .flatMap(ignored -> pollVerificationStatus(verificationUuid, attemptCount + 1))
                )
                .flatMap(result -> {
                    LOG.debugf("Statut de vérification %s: %s (tentative %d)",
                            verificationUuid, result.getStatus(), attemptCount);

                    return switch (result.getStatus()) {
                        case "EN_COURS" ->
                                Uni.createFrom().nullItem()
                                        .onItem().delayIt().by(Duration.ofSeconds(pollingIntervalSeconds))
                                        .flatMap(ignored -> pollVerificationStatus(verificationUuid, attemptCount + 1));
                        case "A_TRAITER" -> {
                            LOG.infof("Vérification %s terminée avec succès", verificationUuid);
                            yield Uni.createFrom().item(result);
                        }
                        case "ERREUR" -> {
                            LOG.errorf("Erreur lors de la vérification %s", verificationUuid);
                            yield Uni.createFrom().failure(
                                    new RuntimeException("Erreur lors de la vérification des documents")
                            );
                        }
                        default -> {
                            LOG.warnf("Statut inconnu %s pour la vérification %s",
                                    result.getStatus(), verificationUuid);
                            yield Uni.createFrom().failure(
                                    new RuntimeException("Statut de vérification inconnu: " + result.getStatus())
                            );
                        }
                    };
                });
    }

    public Uni<JsonObject> verifyIdDocument(String bucketName, String frontImageName, String backImageName) {
        String frontImagePath = "/tmp/" + frontImageName;
        String backImagePath = "/tmp/" + backImageName;

        LOG.debugf("Récupération des fichiers depuis MinIO - bucket: %s, front: %s, back: %s",
                bucketName, frontImageName, backImageName);

        Uni<Path> frontImageUni = minioService.getFile(bucketName, frontImageName, frontImagePath);
        Uni<Path> backImageUni = minioService.getFile(bucketName, backImageName, backImagePath);

        return Uni.combine().all().unis(frontImageUni, backImageUni)
                .asTuple()
                .flatMap(tuple -> {
                    Path frontImage = tuple.getItem1();
                    Path backImage = tuple.getItem2();

                    LOG.debugf("Fichiers récupérés avec succès. Préparation de l'upload vers Danaya");

                    MultipartForm form = MultipartForm.create()
                            .binaryFileUpload("idDocumentFront", frontImage.getFileName().toString(),
                                    frontImage.toString(), "image/jpeg")
                            .binaryFileUpload("idDocumentBack", backImage.getFileName().toString(),
                                    backImage.toString(), "image/jpeg")
                            .attribute("documentType", "CNI")
                            .attribute("verificationsToApply", String.join(",", DEFAULT_VERIFICATIONS));

                    return Uni.createFrom().emitter(em -> {
                        webClient.postAbs(baseUrl + "/v2/clients-files/upload-files")
                                .putHeader("Api-Key", apiKey)
                                .putHeader("Api-Secret", apiSecret)
                                .putHeader("Content-Type", "multipart/form-data")
                                .sendMultipartForm(form, ar -> {
                                    cleanupFiles(frontImage, backImage);
                                    if (ar.succeeded()) {
                                        HttpResponse<io.vertx.core.buffer.Buffer> response = ar.result();
                                        if (response.statusCode() == 200) {
                                            LOG.infof("Upload des documents réussi");
                                            em.complete(response.bodyAsJsonObject());
                                        } else {
                                            String errorMessage = String.format("Erreur API Danaya: %d - %s",
                                                    response.statusCode(), response.statusMessage());
                                            LOG.error(errorMessage);
                                            em.fail(new RuntimeException(errorMessage));
                                        }
                                    } else {
                                        LOG.error("Erreur lors de l'upload des documents", ar.cause());
                                        em.fail(ar.cause());
                                    }
                                });
                    });
                });
    }

    public Uni<DanayaVerificationResult> checkVerificationStatus(String verificationUuid) {
        LOG.debugf("Vérification du statut pour l'UUID: %s", verificationUuid);

        return Uni.createFrom().emitter(em -> {
            String url = baseUrl + "/v2/clients-files/client-file-to-analyze-id/" + verificationUuid;
            LOG.infof("URL: %s", url);
            webClient.getAbs(url)
                    .putHeader("Api-Key", apiKey)
                    .putHeader("Api-Secret", apiSecret)
                    .putHeader("Content-Type", "application/json")
                    .send(ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<io.vertx.core.buffer.Buffer> response = ar.result();
                            if (response.statusCode() == 200) {
                                try {
                                    JsonObject jsonResponse = response.bodyAsJsonObject();
                                    DanayaVerificationResult result = parseDanayaResponse(jsonResponse);
                                    LOG.debugf("Statut récupéré avec succès pour %s: %s",
                                            verificationUuid, result.getStatus());
                                    em.complete(result);
                                } catch (Exception e) {
                                    LOG.error("Erreur lors du parsing de la réponse Danaya", e);
                                    em.fail(new RuntimeException("Erreur parsing réponse Danaya: " + e.getMessage()));
                                }
                            } else if (response.statusCode() == 404) {
                                em.fail(new RuntimeException("404 - Document non trouvé"));
                            } else {
                                String errorMessage = String.format("Erreur API Danaya: %d - %s",
                                        response.statusCode(), response.statusMessage());
                                LOG.error(errorMessage);
                                em.fail(new RuntimeException(errorMessage));
                            }
                        } else {
                            LOG.error("Erreur lors de la vérification du statut", ar.cause());
                            em.fail(ar.cause());
                        }
                    });
        });
    }

    private DanayaVerificationResult parseDanayaResponse(JsonObject response) {
        LOG.debug("Parsing de la réponse Danaya");

        DanayaVerificationResult result = new DanayaVerificationResult();
        result.setId(response.getString("id"));
        result.setCreatedAt(response.getString("createdAt"));
        result.setStatus(response.getString("status"));

        // Parse verification results if documents exist
        if (response.containsKey("documents") && !response.getJsonArray("documents").isEmpty()) {
            JsonObject document = response.getJsonArray("documents")
                    .stream()
                    .map(obj -> (JsonObject) obj)
                    .filter(doc -> "CNI".equals(doc.getString("type")))
                    .findFirst()
                    .orElse(null);

            if (document != null) {
                JsonObject ocrData = document.getJsonObject("ocrExtractedData");
                if (ocrData != null) {
                    LOG.debug("Données OCR trouvées, extraction des informations");
                    result.setOcrData(new DanayaVerificationResult.OcrData(
                            ocrData.getString("first_name"),
                            ocrData.getString("last_name"),
                            ocrData.getString("date_of_birth"),
                            ocrData.getString("document_expiry"),
                            ocrData.getString("nni")
                    ));
                }

                // Parse verification results
                if (document.containsKey("verificationResults")) {
                    LOG.debug("Traitement des résultats de vérification");
                    document.getJsonArray("verificationResults")
                            .stream()
                            .map(obj -> (JsonObject) obj)
                            .forEach(verification -> {
                                String type = verification.getString("type");
                                JsonObject scoring = verification.getJsonObject("scoring");
                                result.addVerificationResult(type, scoring);
                            });
                }
            }
        }

        return result;
    }

    private void cleanupFiles(Path... files) {
        for (Path file : files) {
            try {
                if (file != null && Files.exists(file)) {
                    Files.deleteIfExists(file);
                    LOG.debugf("Fichier temporaire supprimé: %s", file);
                }
            } catch (IOException e) {
                LOG.warnf("Échec de la suppression du fichier temporaire: %s - %s",
                        file, e.getMessage());
            }
        }
    }

    private static class DocumentNotReadyException extends RuntimeException {
        public DocumentNotReadyException() {
            super("Document en cours d'initialisation");
        }
    }
}