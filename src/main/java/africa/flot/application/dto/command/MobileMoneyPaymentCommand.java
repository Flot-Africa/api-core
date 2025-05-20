package africa.flot.application.dto.command;

import africa.flot.domain.model.enums.PaymentMethod;
import africa.flot.domain.model.enums.TransactionStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MobileMoneyPaymentCommand {
    @NotNull
    private UUID loanId;

    @NotNull
    private UUID leadId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotEmpty
    private String provider; // "orange", "mtn", etc.

    @NotEmpty
    private String phoneNumber; // Numéro de téléphone mobile money

    private String otp; // Code OTP (peut être null au départ)

    private String createdBy;

    // Pour le suivi interne
    private PaymentMethod paymentMethod = PaymentMethod.MOBILE_MONEY;
    private TransactionStatus transactionStatus = TransactionStatus.INITIATED;
    private String externalReference; // ID de la transaction HUB2
    private String paymentIntentId; // ID du Payment Intent HUB2
    private String paymentIntentToken; // Token du Payment Intent HUB2
}