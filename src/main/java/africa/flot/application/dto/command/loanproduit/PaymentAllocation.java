package africa.flot.application.dto.command.loanproduit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PaymentAllocation {
    @NotBlank private String transactionType;
    @NotBlank
    private String futureInstallmentAllocationRule;
    @Valid
    @NotEmpty
    private List<PaymentAllocationOrder> paymentAllocationOrder;
}