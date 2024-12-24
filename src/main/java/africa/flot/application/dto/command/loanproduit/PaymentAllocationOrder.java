package africa.flot.application.dto.command.loanproduit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentAllocationOrder {
    @NotNull
    private Integer order;
    @NotBlank
    private String paymentAllocationRule;
}