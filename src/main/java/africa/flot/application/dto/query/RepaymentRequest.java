package africa.flot.application.dto.query;

public record RepaymentRequest(
        String msisdn,
        String provider,
        int amount,
        String otp
) {}
