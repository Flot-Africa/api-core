package africa.flot.infrastructure.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import africa.flot.infrastructure.logging.LoggerUtil;
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
    @Inject
    LoggerUtil logger;

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
        logger.danayaInfo(String.format("Démarrage vérification documents [bucket=%s, front=%s, back=%s]",
                bucketName, frontImageName, backImageName));

        return verifyIdDocument(bucketName, frontImageName, backImageName)
                .flatMap(initialResponse -> {
                    String verificationUuid = initialResponse.getString("id");
                    logger.danayaInfo(String.format("Documents uploadés avec succès [uuid=%s]", verificationUuid));
                    logger.auditAction("SYSTEM", "DOCUMENT_UPLOAD",
                            String.format("Documents uploadés pour vérification [bucket=%s, uuid=%s]",
                                    bucketName, verificationUuid));

                    return Uni.createFrom().nullItem()
                            .onItem().delayIt().by(Duration.ofSeconds(initialDelaySeconds))
                            .flatMap(ignored -> pollVerificationStatus(verificationUuid, 0));
                })
                .onFailure().invoke(error -> {
                    logger.error("Échec de la vérification des documents", error);
                    logger.auditAction("SYSTEM", "DOCUMENT_UPLOAD_FAILED",
                            String.format("Échec upload documents [bucket=%s, error=%s]",
                                    bucketName, error.getMessage()));
                });
    }
    private Uni<DanayaVerificationResult> pollVerificationStatus(String verificationUuid, int attemptCount) {
        if (attemptCount >= maxPollingAttempts) {
            String errorMsg = String.format("Délai d'attente dépassé [uuid=%s, tentatives=%d]",
                    verificationUuid, attemptCount);
            logger.error(errorMsg);
            logger.auditAction("SYSTEM", "VERIFICATION_TIMEOUT",
                    String.format("Timeout vérification [uuid=%s, attempts=%d]",
                            verificationUuid, attemptCount));
            return Uni.createFrom().failure(new RuntimeException(errorMsg));
        }

        return checkVerificationStatus(verificationUuid)
                .onFailure().transform(error -> {
                    if (error.getMessage().contains("404")) {
                        logger.danayaDebug(String.format(
                                "Document en cours d'initialisation [uuid=%s, attente=%ds]",
                                verificationUuid, pollingIntervalSeconds));
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
                    logger.danayaDebug(String.format(
                            "Statut vérification [uuid=%s, status=%s, tentative=%d]",
                            verificationUuid, result.getStatus(), attemptCount));

                    return switch (result.getStatus()) {
                        case "EN_COURS" -> {
                            logger.danayaDebug(String.format(
                                    "Vérification en cours [uuid=%s, tentative=%d]",
                                    verificationUuid, attemptCount));
                            yield Uni.createFrom().nullItem()
                                    .onItem().delayIt().by(Duration.ofSeconds(pollingIntervalSeconds))
                                    .flatMap(ignored -> pollVerificationStatus(verificationUuid, attemptCount + 1));
                        }
                        case "A_TRAITER" -> {
                            logger.danayaInfo(String.format(
                                    "Vérification terminée avec succès [uuid=%s]", verificationUuid));
                            logger.auditAction("SYSTEM", "VERIFICATION_SUCCESS",
                                    String.format("Vérification réussie [uuid=%s]", verificationUuid));
                            yield Uni.createFrom().item(result);
                        }
                        case "ERREUR" -> {
                            String errorMsg = String.format("Échec de la vérification [uuid=%s]", verificationUuid);
                            logger.error(errorMsg);
                            logger.auditAction("SYSTEM", "VERIFICATION_ERROR",
                                    String.format("Échec vérification [uuid=%s]", verificationUuid));
                            yield Uni.createFrom().failure(new RuntimeException(errorMsg));
                        }
                        default -> {
                            String errorMsg = String.format(
                                    "Statut de vérification invalide [uuid=%s, status=%s]",
                                    verificationUuid, result.getStatus());
                            logger.error(errorMsg);
                            yield Uni.createFrom().failure(new RuntimeException(errorMsg));
                        }
                    };
                });
    }
    public Uni<JsonObject> verifyIdDocument(String bucketName, String frontImageName, String backImageName) {
        logger.danayaDebug(String.format(
                "Récupération fichiers MinIO [bucket=%s, front=%s, back=%s]",
                bucketName, frontImageName, backImageName));

        String frontImagePath = "/tmp/" + frontImageName;
        String backImagePath = "/tmp/" + backImageName;

        Uni<Path> frontImageUni = minioService.getFile(bucketName, frontImageName, frontImagePath);
        Uni<Path> backImageUni = minioService.getFile(bucketName, backImageName, backImagePath);

        return Uni.combine().all().unis(frontImageUni, backImageUni)
                .asTuple()
                .flatMap(tuple -> {
                    Path frontImage = tuple.getItem1();
                    Path backImage = tuple.getItem2();

                    logger.danayaDebug("Préparation upload Danaya");

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
                                            logger.danayaInfo("Upload documents réussi");
                                            em.complete(response.bodyAsJsonObject());
                                        } else {
                                            String errorMsg = String.format("Erreur API Danaya [status=%d, message=%s]",
                                                    response.statusCode(), response.statusMessage());
                                            logger.error(errorMsg);
                                            em.fail(new RuntimeException(errorMsg));
                                        }
                                    } else {
                                        logger.error("Erreur upload documents", ar.cause());
                                        em.fail(ar.cause());
                                    }
                                });
                    });
                });
    }

    public Uni<DanayaVerificationResult> checkVerificationStatus(String verificationUuid) {
        logger.danayaDebug(String.format("Vérification statut [uuid=%s]", verificationUuid));

        return Uni.createFrom().emitter(em -> {
            String url = baseUrl + "/v2/clients-files/client-file-to-analyze-id/" + verificationUuid;
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
                                    logger.danayaDebug(String.format(
                                            "Statut récupéré [uuid=%s, status=%s]",
                                            verificationUuid, result.getStatus()));
                                    em.complete(result);
                                } catch (Exception e) {
                                    logger.error("Erreur parsing réponse", e);
                                    em.fail(new RuntimeException("Erreur parsing réponse: " + e.getMessage()));
                                }
                            } else {
                                String errorMsg = String.format("Erreur API [status=%d, message=%s]",
                                        response.statusCode(), response.statusMessage());
                                logger.error(errorMsg);
                                em.fail(new RuntimeException(errorMsg));
                            }
                        } else {
                            logger.error("Erreur vérification statut", ar.cause());
                            em.fail(ar.cause());
                        }
                    });
        });
    }
    private DanayaVerificationResult parseDanayaResponse(JsonObject response) {
        logger.danayaDebug("Parsing réponse Danaya");

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
                    logger.danayaDebug("Données OCR trouvées, extraction des informations");
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
                    logger.danayaDebug("Traitement des résultats de vérification");
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
                    logger.danayaDebug(String.format("Fichier temporaire supprimé [path=%s]", file));
                }
            } catch (IOException e) {
                logger.error(String.format("Échec suppression fichier [path=%s]", file), e);
            }
        }
    }

    private static class DocumentNotReadyException extends RuntimeException {
        public DocumentNotReadyException() {
            super("Document en cours d'initialisation");
        }
    }
}