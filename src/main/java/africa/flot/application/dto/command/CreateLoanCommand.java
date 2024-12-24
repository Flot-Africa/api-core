package africa.flot.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateLoanCommand {
    private Integer productId;
    private Integer clientId;
    private Long principal;
    private Integer loanTermFrequency;
    private Integer numberOfRepayments;
    private Integer repaymentEvery;
    private Double interestRatePerPeriod;
    private String expectedDisbursementDate;
    private String repaymentsStartingFromDate;
    private String submittedOnDate;
    private String status;

    // Informations techniques
    private String dateFormat = "dd MMMM yyyy";
    private String locale = "fr";
    private Integer daysInYearType = 360;
    private Integer amortizationType = 1;
    private Integer interestType = 0;
    private Integer interestCalculationPeriodType = 1;
    private Integer interestRateFrequencyType = 2;
    private String loanScheduleProcessingType = "HORIZONTAL";
    private String transactionProcessingStrategyCode = "principal-interest-penalties-fees-order-strategy";


    // Périodes de grâce
    private Integer graceOnArrearsAgeing = 1;
    private Integer graceOnInterestCharged = 1;
    private Integer graceOnInterestPayment = 1;
    private Integer graceOnPrincipalPayment = 1;

    private String loanType;
    private Long maxOutstandingLoanBalance;
    private Boolean enableInstallmentLevelDelinquency;

    private List<DisbursementDataDTO> disbursementData;

}