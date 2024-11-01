package africa.flot.infrastructure.service.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import africa.flot.infrastructure.logging.LoggerUtil;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class DanayaResponseParser {

    @Inject
    LoggerUtil logger;

    public static DanayaVerificationResult parseDanayaResponse(JsonObject response) {
        LoggerUtil logger = new LoggerUtil(); // Créez une instance si nécessaire
        logger.danayaDebug("Parsing Danaya response");

        DanayaVerificationResult result = new DanayaVerificationResult();
        result.setId(UUID.fromString(response.getString("clientFileToAnalyzeId")));
        result.setCreatedAt(response.getString("createdAt"));
        result.setStatus(response.getString("status"));

        DanayaVerificationResult.PersonalInfo personalInfo = new DanayaVerificationResult.PersonalInfo();
        result.setPersonalInfo(personalInfo);

        DanayaVerificationResult.VerificationScores verificationScores = new DanayaVerificationResult.VerificationScores();
        result.setVerificationScores(verificationScores);

        if (response.containsKey("documents") && !response.getJsonArray("documents").isEmpty()) {
            JsonObject document = response.getJsonArray("documents").stream()
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
                    document.getJsonArray("verificationResults").stream()
                            .map(obj -> (JsonObject) obj)
                            .forEach(verification -> {
                                String type = verification.getString("type");

                                if ("EXPIRATION_CHECK".equals(type)) {
                                    JsonObject scoring = verification.getJsonObject("scoring");
                                    verificationScores.setExpiration(scoring.getString("score"));
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
}
