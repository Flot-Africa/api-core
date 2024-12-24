package africa.flot.application.dto.command.loanproduit;

import lombok.Data;

@Data
public class LoanAllowAttributeOverrides {
    private Boolean amortizationType;
    private Boolean interestType;
    private Boolean graceOnArrearsAgeing;
    private Boolean transactionProcessingStrategyCode;
}