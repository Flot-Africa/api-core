package africa.flot.application.dto.query;

import lombok.Data;

@Data
public class KYBStatus {
    private boolean isVerified;
    private int verificationProgress;
    private String message;

    public static KYBStatus of(boolean isVerified, int progress, String message) {
        KYBStatus status = new KYBStatus();
        status.setVerified(isVerified);
        status.setVerificationProgress(progress);
        status.setMessage(message);
        return status;
    }
}