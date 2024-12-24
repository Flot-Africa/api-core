package africa.flot.application.dto.command.loanproduit;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentChannelMapping {
    @NotNull private Long paymentTypeId;
    @NotNull
    private Long fundSourceAccountId;
}
