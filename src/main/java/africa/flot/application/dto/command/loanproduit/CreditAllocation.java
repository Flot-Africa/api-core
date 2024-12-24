package africa.flot.application.dto.command.loanproduit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreditAllocation {
    @NotBlank
    private String transactionType;
    @Valid
    @NotEmpty
    private List<CreditAllocationRule> creditAllocationOrder;
}