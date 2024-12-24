package africa.flot.application.dto.command.loanproduit;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChargeOffMapping {
    @NotNull private Long chargeOffReasonCodeValueId;
    @NotNull
    private Long expenseGLAccountId;
}