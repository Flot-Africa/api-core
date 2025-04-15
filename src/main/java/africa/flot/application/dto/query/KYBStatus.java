package africa.flot.application.dto.query;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
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