package africa.flot.application.dto.query;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class DanayaVerificationResult {
    private String id;
    private String createdAt;
    private String status;
    private OcrData ocrData;
    private Map<String, JsonObject> verificationResults = new HashMap<>();

    public static class OcrData {
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String documentExpiry;
        private String nni;

        public OcrData(String firstName, String lastName, String dateOfBirth,
                       String documentExpiry, String nni) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.dateOfBirth = dateOfBirth;
            this.documentExpiry = documentExpiry;
            this.nni = nni;
        }

        // Getters
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getDateOfBirth() { return dateOfBirth; }
        public String getDocumentExpiry() { return documentExpiry; }
        public String getNni() { return nni; }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OcrData getOcrData() { return ocrData; }
    public void setOcrData(OcrData ocrData) { this.ocrData = ocrData; }
    public Map<String, JsonObject> getVerificationResults() { return verificationResults; }
    public void addVerificationResult(String type, JsonObject result) {
        verificationResults.put(type, result);
    }
}
