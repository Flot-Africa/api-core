package africa.flot.application.dto.command.loanproduit;

import lombok.Data;

@Data
public class FeeIncomeMapping {
    private Long chargeId;
    private Long incomeAccountId;
    private Charge charge;
    private GLAccount incomeAccount;
}