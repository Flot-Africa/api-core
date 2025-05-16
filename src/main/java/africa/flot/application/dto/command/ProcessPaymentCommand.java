package africa.flot.application.dto.command;

import africa.flot.domain.model.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProcessPaymentCommand {
    private UUID loanId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;
    private String externalReference;  // Référence bancaire, ID transaction, etc.
    private String notes;
    private String createdBy = "SYSTEM";
}
