package africa.flot.infrastructure.service.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import africa.flot.infrastructure.logging.LoggerUtil;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DanayaApiClient {

    @Inject
    LoggerUtil logger;

    private final WebClient webClient;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.url")
    String baseUrl;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.api-secret")
    String apiSecret;

    private static final List<String> DEFAULT_VERIFICATIONS = Arrays.asList("DB_CHECK", "EXPIRATION_CHECK", "TEMPLATE_CHECK");

    @Inject
    public DanayaApiClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout(5000)
                .setIdleTimeout(10000);
        this.webClient = WebClient.create(vertx, options);
    }

    public Uni<JsonObject> uploadIdDocuments(Path frontImage, Path backImage) {
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

    public Uni<DanayaVerificationResult> getVerificationStatus(UUID verificationUuid) {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            logger.danayaDebug("Mode développement détecté - utilisation de données simulées.");
            return Uni.createFrom().item(loadMockResponse())
                    .map(DanayaResponseParser::parseDanayaResponse);
        } else {
            return Uni.createFrom().emitter(emitter -> {
                String url = baseUrl + "/v2/clients-files/client-file-to-analyze-id/" + verificationUuid;
                logger.danayaDebug("Appel à l'API Danaya [url=" + url + "]");

                webClient.getAbs(url)
                        .putHeader("Api-Key", apiKey)
                        .putHeader("Api-Secret", apiSecret)
                        .send(ar -> {
                            if (ar.succeeded()) {
                                HttpResponse<Buffer> response = ar.result();
                                if (response.statusCode() == 200) {
                                    DanayaVerificationResult result = DanayaResponseParser.parseDanayaResponse(response.bodyAsJsonObject());
                                    emitter.complete(result);
                                } else {
                                    emitter.fail(new RuntimeException(String.format("Erreur API [status=%d, message=%s]", response.statusCode(), response.statusMessage())));
                                }
                            } else {
                                emitter.fail(ar.cause());
                            }
                        });
            });
        }
    }

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
                }""";
        return new JsonObject(jsonResponse);
    }
}
