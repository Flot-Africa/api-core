package africa.flot.application.dto.command;

import africa.flot.domain.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProcessPaymentCommand {

    private UUID loanId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private Double amount;

    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    private String externalReference;

    private String notes;

    private String createdBy;

    // Champs pour Mobile Money
    private String paymentProvider; // "orange", "mtn", etc.
    private String paymentPhoneNumber; // Numéro de téléphone
    private String paymentIntentId; // ID de l'intent HUB2
    private String paymentTransactionId; // ID de la transaction HUB2
}