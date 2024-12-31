package africa.flot.application.dto.response;

import java.math.BigDecimal;

public record RepaymentResponseDTO(
        String transactionId,
        BigDecimal amount,
        String date,
        String status
) {}
