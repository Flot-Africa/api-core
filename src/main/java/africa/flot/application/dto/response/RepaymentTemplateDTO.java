package africa.flot.application.dto.response;

import java.math.BigDecimal;

public record RepaymentTemplateDTO(
        BigDecimal amount,
        BigDecimal principalPortion,
        String date,
        String currency
) {}

