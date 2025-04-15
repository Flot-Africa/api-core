package africa.flot.infrastructure.service.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import africa.flot.application.dto.query.KYBStatus;
import africa.flot.application.exceptions.DocumentNotReadyException;
import africa.flot.infrastructure.repository.DanayaVerificationRepository;
import africa.flot.infrastructure.repository.KYBRepository;
import africa.flot.domain.model.DanayaVerificationResults;
import africa.flot.domain.model.KYBDocuments;
import africa.flot.infrastructure.logging.LoggerUtil;
import africa.flot.infrastructure.minio.MinioService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DanayaService {
    @Inject
    LoggerUtil logger;

    @Inject
    KYBRepository kybRepository;

    @Inject
    MinioService minioService;

    @Inject
    DanayaVerificationRepository danayaVerificationRepository;

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

    private static final List<String> DEFAULT_VERIFICATIONS = Arrays.asList("DB_CHECK", "EXPIRATION_CHECK", "TEMPLATE_CHECK");
    private JsonObject loadMockResponse() {
        String jsonResponse = """
                {
                              "id": 1103,
                              "createdAt": "2024-10-25 11:21:06",
                              "clientFileToAnalyzeId": "73b9c527-119e-41a8-a00d-007b64b058cf",
                              "status": "A_TRAITER",
                              "company": {
                                  "id": 15,
                                  "name": "Flot",
                                  "verifications": []
                              },
                              "documents": [
                                  {
                                      "id": 1463,
                                      "type": "CNI",
                                      "frontUrl": "https://storage.googleapis.com/dayana-mvp.appspot.com/CNI_front_82a307b0-0097-4759-81c9-8081ef786972?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=firebase-adminsdk-aanmr%40dayana-mvp.iam.gserviceaccount.com%2F20241026%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20241026T134213Z&X-Goog-Expires=1800&X-Goog-SignedHeaders=host&X-Goog-Signature=293cc4f913efd1f24361a8517fb3c29f157de19552c92ab1be8881dd728520ac5b2d72a3b5f59167e1851f51d13832e88a355f347707098146987d382cdc9635babd0cbf97ede8766c2d9a632a1a90612d94f497cd605850a0e30c9ee22252fc0de5d4a8f7b4cb293c3852d5d2381308fbc2a8f8358b1b6bfe9ca0348b6e8f132cfc7d4fede38a99efe4a8a0459b4533ab4acf096fd4d96c5f675844ea180cbf7341879bbfe154759831e88e9f5816b9171eb15c8019624db7a30cbfd66b5fcac8c91c8d10951fd84ce440d6e20e63bbbc7296fa7c1e8343bf63092d61a6e57a7e6a78e4ceadae5ca320196d692a634a564587fc9c4cb831894c20828c7a8355",
                                      "backUrl": "https://storage.googleapis.com/dayana-mvp.appspot.com/CNI_back_509a7f4b-3d31-4c1a-bb67-fcbf009d60cd?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=firebase-adminsdk-aanmr%40dayana-mvp.iam.gserviceaccount.com%2F20241026%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20241026T134213Z&X-Goog-Expires=1800&X-Goog-SignedHeaders=host&X-Goog-Signature=8399999edb2d39e5e3aa4b3dcf0d55231459adbdccdef825d7bde6fadf91fad5e986e9901bca28f3fcabf9eee1ad1dd6f46c56871cbb81cb852cbb5654d952d38c69035ca273d1cb8978fc929abe407ed0dc37875e3599c47e73177bebef57bc8f4109c828e9b4e8fbb049e0d2fdac52531ea64fbb18284764f8c3510fa311bf3882a1ae92a044704dffb170a07c04e89790ba3085aac69375a10567f915df0fbbb040def79426e71b15ec7dac562b093f9a6cc570d5ead9f288fd2ae5ed146f74d6be8390a4a5a2831a162fade66d9253ab7376f95344df6242b594f34506ce915f09b4131514000dce5d5dafb24a6023063e6d8f0dee8b4ce52a18626897dc",
                                      "ocrExtractedData": {
                                          "mrz": "IDCIVCI0045827<787<<<<<<<<<<<<\\n9912245M3208047CIV119902193137\\nYAO<<NGORAN<ELOGE<<<<<<<<<<<<<",
                                          "nni": "11990219313",
                                          "image": null,
                                          "state": null,
                                          "gender": "M",
                                          "height": "1,85",
                                          "message": null,
                                          "ocr_raw": null,
                                          "last_name": "YAO",
                                          "first_name": "N'GORAN ELOGE",
                                          "profession": "ETUDIANT(E)",
                                          "father_name": null,
                                          "mother_name": null,
                                          "nationality": "IVOIRIENNE",
                                          "country_code": null,
                                          "date_of_birth": "24/12/1999",
                                          "document_code": "CARTE NATIONALE D'IDENTITÉ",
                                          "place_of_birth": "TOUMODI S/P (CIV)",
                                          "document_expiry": "04/08/2032",
                                          "document_number": "CI004582778",
                                          "father_birthday": null,
                                          "mother_birthday": null,
                                          "personal_number": null,
                                          "permanent_address": null,
                                          "date_of_birth_yyyy_mm_dd": null,
                                          "document_expiry_yyyy_mm_dd": null
                                      },
                                      "verificationResults": [
                                          {
                                              "id": "fbe2b14c-e262-4f76-a1ec-b9e38aed55d7",
                                              "status": "EXECUTED",
                                              "lastRunDate": "2024-10-25 11:21:08",
                                              "type": "EXPIRATION_CHECK",
                                              "scoring": {
                                                  "type": "ExpiryDateScoring",
                                                  "score": "VALID"
                                              },
                                              "clientFileDocumentId": 1463
                                          },
                                          {
                                              "id": "ccc1033f-7020-48f2-ab28-c74ec0c00d3e",
                                              "status": "EXECUTED",
                                              "lastRunDate": "2024-10-25 11:21:08",
                                              "type": "DB_CHECK",
                                              "scoring": {
                                                  "type": "PersonInfoScoring",
                                                  "id": 655,
                                                  "nni": "11990219313",
                                                  "iDCardNumber": null,
                                                  "phoneNumber": null,
                                                  "firstNameMatchingScore": 100,
                                                  "lastNameMatchingScore": 100,
                                                  "dateOfBirthMatchingScore": 100,
                                                  "genderMatchingScore": 100,
                                                  "phoneNumberMatchingScore": 0,
                                                  "comparedWithRealData": true,
                                                  "rawData": "{\\"dateOfBirth\\":\\"1999-12-24\\",\\"UIN\\":\\"11990219313\\",\\"LAST_NAME\\":\\"YAO\\",\\"FIRST_NAME\\":\\"N'GORAN ELOGE\\",\\"GENDER\\":\\"M\\",\\"BIRTH_DATE\\":\\"1999-12-24\\",\\"FATHER_FIRST_NAME\\":\\"KOUASSI VICTOR\\",\\"FATHER_LAST_NAME\\":\\"YAO\\",\\"FATHER_BIRTH_DATE\\":\\"1945-01-01\\",\\"MOTHER_FIRST_NAME\\":\\"AMOIN MARGUERITE\\",\\"MOTHER_LAST_NAME\\":\\"KASSE\\",\\"MOTHER_BIRTH_DATE\\":\\"XX/XX/XXXX\\",\\"RESIDENCE_ADR_1\\":\\"MERMOZ\\",\\"NATIONALITY\\":\\"CIV\\",\\"ID_CARD_NUMBER\\":\\"I007066695\\",\\"BIRTH_TOWN\\":\\"TOUMODI S/P\\",\\"BIRTH_COUNTRY\\":\\"CIV\\",\\"SPOUSE_NAME\\":\\"\\",\\"RESIDENCE_TOWN\\":\\"COCODY\\",\\"FATHER_UIN\\":\\"\\",\\"MOTHER_UIN\\":\\"\\",\\"RESIDENCE_ADR_2\\":\\"\\"}"
                                              },
                                              "clientFileDocumentId": 1463
                                          },
                                          {
                                              "type": "DB_CHECK",
                                              "scoring": {
                                                  "type": "PersonInfoScoring",
                                                  "id": 655,
                                                  "nni": "11990219313",
                                                  "iDCardNumber": null,
                                                  "phoneNumber": null,
                                                  "firstNameMatchingScore": 100,
                                                  "lastNameMatchingScore": 100,
                                                  "dateOfBirthMatchingScore": 100,
                                                  "genderMatchingScore": 100,
                                                  "phoneNumberMatchingScore": 0,
                                                  "comparedWithRealData": true,
                                                  "rawData": "{\\"dateOfBirth\\":\\"1999-12-24\\",\\"UIN\\":\\"11990219313\\",\\"LAST_NAME\\":\\"YAO\\",\\"FIRST_NAME\\":\\"N'GORAN ELOGE\\",\\"GENDER\\":\\"M\\",\\"BIRTH_DATE\\":\\"1999-12-24\\",\\"FATHER_FIRST_NAME\\":\\"KOUASSI VICTOR\\",\\"FATHER_LAST_NAME\\":\\"YAO\\",\\"FATHER_BIRTH_DATE\\":\\"1945-01-01\\",\\"MOTHER_FIRST_NAME\\":\\"AMOIN MARGUERITE\\",\\"MOTHER_LAST_NAME\\":\\"KASSE\\",\\"MOTHER_BIRTH_DATE\\":\\"XX/XX/XXXX\\",\\"RESIDENCE_ADR_1\\":\\"MERMOZ\\",\\"NATIONALITY\\":\\"CIV\\",\\"ID_CARD_NUMBER\\":\\"I007066695\\",\\"BIRTH_TOWN\\":\\"TOUMODI S/P\\",\\"BIRTH_COUNTRY\\":\\"CIV\\",\\"SPOUSE_NAME\\":\\"\\",\\"RESIDENCE_TOWN\\":\\"COCODY\\",\\"FATHER_UIN\\":\\"\\",\\"MOTHER_UIN\\":\\"\\",\\"RESIDENCE_ADR_2\\":\\"\\"}"
                                              },
                                              "clientFileDocumentId": 1463
                                          }
                                      ]
                                  }
                              ]
                          }
        """;
        return new JsonObject(jsonResponse);
    }
    @Inject
    public DanayaService(Vertx vertx) {
        WebClientOptions options = new WebClientOptions().setConnectTimeout(5000).setIdleTimeout(10000);
        this.webClient = WebClient.create(vertx, options);
    }

    @WithSession
    public Uni<DanayaVerificationResult> verifyIdDocumentWithPolling(String bucketName, String frontImageName, String backImageName, UUID leadId) {
        return danayaVerificationRepository.findByLeadId(leadId)
                .flatMap(optionalVerification -> {
                    if (optionalVerification.isPresent() && "VALID".equals(optionalVerification.get().getStatus())) {
                        logger.danayaInfo(String.format("Lead déjà vérifié [leadId=%s]. Retour du résultat existant.", leadId));
                        return Uni.createFrom().item(optionalVerification.get().toDanayaVerificationResult());
                    }

                    logger.danayaInfo(String.format("Démarrage vérification documents [bucket=%s, front=%s, back=%s, leadId=%s]", bucketName, frontImageName, backImageName, leadId));

                    return verifyIdDocument(bucketName, frontImageName, backImageName)
                            .flatMap(initialResponse -> {
                                String verificationId = initialResponse.getString("id");
                                logger.danayaDebug("Initial response: " + initialResponse.encodePrettily());
                                if (verificationId == null || verificationId.isEmpty()) {
                                    String errorMessage = String.format("ID de vérification manquant pour le lead [leadId=%s].", leadId);
                                    logger.error(errorMessage);
                                    return Uni.createFrom().failure(new RuntimeException(errorMessage));
                                }
                                return startVerificationPolling(UUID.fromString(verificationId), leadId);
                            })
                            .onFailure().invoke(error -> logVerificationFailure(bucketName, error));
                })
                .onFailure().invoke(error -> logger.error("Erreur lors de la vérification des documents", error));
    }

    private Uni<DanayaVerificationResult> startVerificationPolling(UUID verificationUuid, UUID leadId) {
        return Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofSeconds(initialDelaySeconds))
                .flatMap(ignored -> pollVerificationStatus(verificationUuid, 0))
                .flatMap(result -> updateKYBStatus(result, leadId));
    }

    @WithTransaction
    protected Uni<DanayaVerificationResult> updateKYBStatus(DanayaVerificationResult result, UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .flatMap(optionalKybDoc -> {
                    KYBDocuments kybDoc = optionalKybDoc.orElseGet(() -> createNewKYBDocument(leadId));
                    updateKYBDocumentFromResult(kybDoc, result);
                    return kybRepository.persistAndFlush(kybDoc)
                            .flatMap(savedDoc -> saveVerificationResult(result, leadId))
                            .map(savedResult -> logKYBUpdateSuccess(leadId, kybDoc, result));
                });
    }

    private KYBDocuments createNewKYBDocument(UUID leadId) {
        KYBDocuments newDoc = new KYBDocuments();
        newDoc.setId(UUID.randomUUID());
        newDoc.setLeadId(leadId);
        return newDoc;
    }

    private DanayaVerificationResult logKYBUpdateSuccess(UUID leadId, KYBDocuments kybDoc, DanayaVerificationResult result) {
        logger.danayaInfo(String.format("KYB mis à jour [leadId=%s, progression=%d]", leadId, kybDoc.getCniProgressionVerification()));
        return result;
    }

    private void logVerificationFailure(String bucketName, Throwable error) {
        logger.error("Échec de la vérification des documents", error);
        logger.auditAction("SYSTEM", "DOCUMENT_UPLOAD_FAILED", String.format("Échec upload documents [bucket=%s, error=%s]", bucketName, error.getMessage()));
    }

    @WithTransaction
    public Uni<Void> saveVerificationResult(DanayaVerificationResult result, UUID leadId) {
        logger.danayaDebug("Enregistrement du résultat de vérification pour leadId=" + leadId);

        return danayaVerificationRepository.findByLeadId(leadId)
                .flatMap(optionalExistingResult -> {
                    if (optionalExistingResult.isPresent()) {
                        // Si une vérification existe déjà, on met à jour ses champs sans toucher à l'ID
                        DanayaVerificationResults existingVerification = optionalExistingResult.get();
                        existingVerification.updateFromVerificationResult(result);

                        return existingVerification.persistAndFlush()
                                .onItem().invoke(() -> logger.danayaInfo("Résultat de vérification mis à jour pour leadId=" + leadId))
                                .onFailure().invoke(e -> logger.error("Échec de la mise à jour du résultat de vérification pour leadId=" + leadId, e))
                                .replaceWith(Uni.createFrom().voidItem());
                    } else {
                        // Création d'une nouvelle vérification
                        DanayaVerificationResults newVerification = new DanayaVerificationResults();
                        newVerification.setLeadId(leadId);
                        newVerification.setId(UUID.randomUUID()); // Générer un nouvel ID uniquement pour les nouvelles vérifications
                        newVerification.updateFromVerificationResult(result);

                        return newVerification.persistAndFlush()
                                .onItem().invoke(() -> logger.danayaInfo("Nouveau résultat de vérification enregistré pour leadId=" + leadId))
                                .onFailure().invoke(e -> logger.error("Échec de l'enregistrement du nouveau résultat de vérification pour leadId=" + leadId, e))
                                .replaceWith(Uni.createFrom().voidItem());
                    }
                });
    }

    private static @NotNull DanayaVerificationResults getDanayaVerificationResults(UUID leadId, Optional<DanayaVerificationResults> optionalExistingResult) {
        DanayaVerificationResults verificationResult;
        if (optionalExistingResult.isPresent()) {
            // Utiliser l'entité existante
            verificationResult = optionalExistingResult.get();
        } else {
            // Créer une nouvelle instance si aucune n'existe
            verificationResult = new DanayaVerificationResults();
            verificationResult.setLeadId(leadId);
        }
        return verificationResult;
    }

    private Uni<DanayaVerificationResult> pollVerificationStatus(UUID verificationUuid, int attemptCount) {
        if (attemptCount >= maxPollingAttempts) {
            return logAndFailPollingTimeout(verificationUuid, attemptCount);
        }

        return checkVerificationStatus(verificationUuid)
                .onFailure().transform(this::handleVerificationStatusFailure)
                .onFailure(DocumentNotReadyException.class)
                .recoverWithUni(() -> retryPollingStatus(verificationUuid, attemptCount))
                .flatMap(this::handleVerificationStatusResult);
    }

    private Uni<DanayaVerificationResult> logAndFailPollingTimeout(UUID verificationUuid, int attemptCount) {
        String errorMsg = String.format("Délai d'attente dépassé [uuid=%s, tentatives=%d]", verificationUuid, attemptCount);
        logger.error(errorMsg);
        logger.auditAction("SYSTEM", "VERIFICATION_TIMEOUT", String.format("Timeout vérification [uuid=%s, attempts=%d]", verificationUuid, attemptCount));
        return Uni.createFrom().failure(new RuntimeException(errorMsg));
    }

    private Throwable handleVerificationStatusFailure(Throwable error) {
        if (error.getMessage().contains("404")) {
            logger.danayaDebug("Document en cours d'initialisation, nouvelle tentative après délai.");
            return new DocumentNotReadyException();
        }
        return error;
    }

    private Uni<DanayaVerificationResult> retryPollingStatus(UUID verificationUuid, int attemptCount) {
        return Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofSeconds(pollingIntervalSeconds))
                .flatMap(ignored -> pollVerificationStatus(verificationUuid, attemptCount + 1));
    }

    private Uni<DanayaVerificationResult> handleVerificationStatusResult(DanayaVerificationResult result) {
        String status = result.getStatus();
        logger.danayaDebug(String.format("Statut vérification [uuid=%s, status=%s]", result.getId(), status));
        if ("EN_COURS".equals(status)) {
            return retryPollingStatus(result.getId(), 0);
        } else if ("VALID".equals(status)) {
            logger.auditAction("SYSTEM", "VERIFICATION_SUCCESS", String.format("Vérification réussie [uuid=%s]", result.getId()));
            return Uni.createFrom().item(result);
        } else if ("ERREUR".equals(status)) {
            return Uni.createFrom().failure(new RuntimeException(String.format("Échec de la vérification [uuid=%s]", result.getId())));
        } else {
            return Uni.createFrom().failure(new RuntimeException(String.format("Statut de vérification invalide [uuid=%s, status=%s]", result.getId(), status)));
        }
    }

    public Uni<JsonObject> verifyIdDocument(String bucketName, String frontImageName, String backImageName) {
        return retrieveFilesFromMinio(bucketName, frontImageName, backImageName)
                .flatMap(paths -> uploadDocumentsToDanaya(paths.getItem1(), paths.getItem2()));
    }

    private Uni<Tuple2<Path, Path>> retrieveFilesFromMinio(String bucketName, String frontImageName, String backImageName) {
        return Uni.combine().all().unis(
                minioService.getFile(bucketName, frontImageName, "/tmp/" + frontImageName),
                minioService.getFile(bucketName, backImageName, "/tmp/" + backImageName)
        ).asTuple();
    }

    private Uni<JsonObject> uploadDocumentsToDanaya(Path frontImage, Path backImage) {
        MultipartForm form = createMultipartForm(frontImage, backImage);
        return Uni.createFrom().emitter(em -> webClient.postAbs(baseUrl + "/v2/clients-files/upload-files")
                .putHeader("Api-Key", apiKey)
                .putHeader("Api-Secret", apiSecret)
                .sendMultipartForm(form, ar -> handleUploadResponse(em, frontImage, backImage, ar)));
    }

    private MultipartForm createMultipartForm(Path frontImage, Path backImage) {
        return MultipartForm.create()
                .binaryFileUpload("idDocumentFront", frontImage.getFileName().toString(), frontImage.toString(), "image/jpeg")
                .binaryFileUpload("idDocumentBack", backImage.getFileName().toString(), backImage.toString(), "image/jpeg")
                .attribute("documentType", "CNI")
                .attribute("verificationsToApply", String.join(",", DEFAULT_VERIFICATIONS));
    }

    private void handleUploadResponse(UniEmitter<? super JsonObject> emitter, Path frontImage, Path backImage, AsyncResult<HttpResponse<Buffer>> ar) {
        cleanupFiles(frontImage, backImage);
        if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response.statusCode() == 200) {
                emitter.complete(response.bodyAsJsonObject());
            } else {
                emitter.fail(new RuntimeException(String.format("Erreur API Danaya [status=%d, message=%s]", response.statusCode(), response.statusMessage())));
            }
        } else {
            emitter.fail(ar.cause());
        }
    }

    private void cleanupFiles(Path... files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
                logger.danayaDebug("Fichier temporaire supprimé [path=" + file + "]");
            } catch (IOException e) {
                logger.error("Échec suppression fichier [path=" + file + "]", e);
            }
        }
    }


    public Uni<DanayaVerificationResult> checkVerificationStatus(UUID verificationUuid) {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            logger.danayaDebug("Mode développement détecté - utilisation de données simulées.");
            // Create Uni in the correct context
            return Uni.createFrom().emitter(emitter ->
                    Vertx.currentContext().runOnContext(v -> {
                        JsonObject mockResponse = loadMockResponse();
                        DanayaVerificationResult result = parseDanayaResponse(mockResponse);
                        emitter.complete(result);
                    })
            );
        } else {
            return Uni.createFrom().emitter(emitter -> {
                // Ensure we're on the Vert.x event loop thread
                Vertx.currentContext().runOnContext(v -> {
                    String url = baseUrl + "/v2/clients-files/client-file-to-analyze-id/" + verificationUuid;
                    logger.danayaDebug("Appel à l'API Danaya [url=" + url + "]");

                    webClient.getAbs(url)
                            .putHeader("Api-Key", apiKey)
                            .putHeader("Api-Secret", apiSecret)
                            .send(ar -> handleVerificationStatusResponse(emitter, verificationUuid, ar));
                });
            });
        }
    }

    private void handleVerificationStatusResponse(UniEmitter<? super DanayaVerificationResult> emitter, UUID verificationUuid, AsyncResult<HttpResponse<Buffer>> ar) {
        if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response.statusCode() == 200) {
                DanayaVerificationResult result = parseDanayaResponse(response.bodyAsJsonObject());
                emitter.complete(result);
            } else {
                emitter.fail(new RuntimeException(String.format("Erreur API [status=%d, message=%s]", response.statusCode(), response.statusMessage())));
            }
        } else {
            emitter.fail(ar.cause());
        }
    }

    private DanayaVerificationResult parseDanayaResponse(JsonObject response) {
        logger.danayaDebug("Parsing Danaya response");

        DanayaVerificationResult result = new DanayaVerificationResult();
        result.setId(UUID.fromString(response.getString("clientFileToAnalyzeId")));
        result.setCreatedAt(response.getString("createdAt"));


        DanayaVerificationResult.PersonalInfo personalInfo = new DanayaVerificationResult.PersonalInfo();
        result.setPersonalInfo(personalInfo);

        DanayaVerificationResult.VerificationScores verificationScores = new DanayaVerificationResult.VerificationScores();
        result.setVerificationScores(verificationScores);

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
                    logger.danayaDebug("OCR data found, extracting information");

                    DanayaVerificationResult.Identity identity = new DanayaVerificationResult.Identity();
                    identity.setFirstName(ocrData.getString("first_name"));
                    identity.setLastName(ocrData.getString("last_name"));
                    identity.setDateOfBirth(ocrData.getString("date_of_birth"));
                    identity.setDocumentExpiry(ocrData.getString("document_expiry"));
                    identity.setNni(ocrData.getString("nni"));
                    identity.setGender(ocrData.getString("gender"));
                    identity.setPlaceOfBirth(ocrData.getString("place_of_birth"));
                    identity.setNationality(ocrData.getString("nationality"));
                    identity.setDocumentNumber(ocrData.getString("document_number"));

                    personalInfo.setIdentity(identity);

                    DanayaVerificationResult.FamilyInfo familyInfo = new DanayaVerificationResult.FamilyInfo();
                    personalInfo.setFamilyInfo(familyInfo);

                    DanayaVerificationResult.Residence residence = new DanayaVerificationResult.Residence();
                    personalInfo.setResidence(residence);
                }

                if (document.containsKey("verificationResults")) {
                    logger.danayaDebug("Processing verification results");
                    document.getJsonArray("verificationResults")
                            .stream()
                            .map(obj -> (JsonObject) obj)
                            .forEach(verification -> {
                                String type = verification.getString("type");

                                if ("EXPIRATION_CHECK".equals(type)) {
                                    JsonObject scoring = verification.getJsonObject("scoring");
                                    verificationScores.setExpiration(scoring.getString("score"));
                                    result.setStatus(scoring.getString("score"));
                                } else if ("DB_CHECK".equals(type)) {
                                    JsonObject scoring = verification.getJsonObject("scoring");
                                    DanayaVerificationResult.DBCheckScores dbCheckScores = new DanayaVerificationResult.DBCheckScores();
                                    dbCheckScores.setFirstName(scoring.getInteger("firstNameMatchingScore", 0));
                                    dbCheckScores.setLastName(scoring.getInteger("lastNameMatchingScore", 0));
                                    dbCheckScores.setDateOfBirth(scoring.getInteger("dateOfBirthMatchingScore", 0));
                                    dbCheckScores.setGender(scoring.getInteger("genderMatchingScore", 0));
                                    verificationScores.setDbCheck(dbCheckScores);

                                    String rawDataString = scoring.getString("rawData");
                                    if (rawDataString != null) {
                                        JsonObject rawData = new JsonObject(rawDataString);

                                        DanayaVerificationResult.Residence residence = personalInfo.getResidence();
                                        residence.setAddress(rawData.getString("RESIDENCE_ADR_1"));
                                        residence.setTown(rawData.getString("RESIDENCE_TOWN"));

                                        DanayaVerificationResult.ParentInfo father = new DanayaVerificationResult.ParentInfo();
                                        father.setFirstName(rawData.getString("FATHER_FIRST_NAME"));
                                        father.setLastName(rawData.getString("FATHER_LAST_NAME"));
                                        father.setBirthDate(rawData.getString("FATHER_BIRTH_DATE"));
                                        father.setUin(rawData.getString("FATHER_UIN"));
                                        personalInfo.getFamilyInfo().setFather(father);

                                        DanayaVerificationResult.ParentInfo mother = new DanayaVerificationResult.ParentInfo();
                                        mother.setFirstName(rawData.getString("MOTHER_FIRST_NAME"));
                                        mother.setLastName(rawData.getString("MOTHER_LAST_NAME"));
                                        mother.setBirthDate(rawData.getString("MOTHER_BIRTH_DATE"));
                                        mother.setUin(rawData.getString("MOTHER_UIN"));
                                        personalInfo.getFamilyInfo().setMother(mother);

                                        personalInfo.getFamilyInfo().setSpouseName(rawData.getString("SPOUSE_NAME"));
                                    }
                                }
                            });
                }
            }
        }
        return result;
    }

    private void updateKYBDocumentFromResult(KYBDocuments kybDoc, DanayaVerificationResult result) {
        kybDoc.setCniUploadee(true);
        kybDoc.setCniProgressionVerification(result.isValid() ? 100 : 0);

        if (result.isValid()) {
            kybDoc.setPhotoIdentiteUploade(true);
            logger.danayaInfo("Documents KYB validés");
        }
    }

    public Uni<KYBStatus> getKYBStatus(UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .flatMap(optionalKybDoc -> {
                    if (optionalKybDoc.isPresent()) {
                        KYBDocuments kyb = optionalKybDoc.get();
                        return isKYBVerified(leadId)
                                .map(isVerified -> KYBStatus.of(isVerified, kyb.getCniProgressionVerification(), isVerified ? "Vérification KYB complète" : "Vérification KYB en cours"));
                    }
                    return Uni.createFrom().item(KYBStatus.of(false, 0, "Aucune vérification KYB trouvée"));
                });
    }

    public Uni<Boolean> isKYBVerified(UUID leadId) {
        return kybRepository.findByLeadId(leadId)
                .map(optionalKyb -> optionalKyb.map(kyb -> kyb.getCniProgressionVerification() == 100).orElse(false));
    }
}