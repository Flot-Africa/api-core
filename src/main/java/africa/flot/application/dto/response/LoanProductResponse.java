package africa.flot.application.dto.response;

import lombok.Data;

@Data
public class LoanProductResponse {
    private Long id;
    private String name;
    private String shortName;
    private Double minPrincipal;
    private Double maxPrincipal;
    private Integer numberOfRepayments;
    private Double interestRatePerPeriod;
    private String repaymentFrequencyType;
    private String interestRateFrequencyType;
    private String amortizationType;
    private String interestType;
    private String interestCalculationPeriodType;
    private String transactionProcessingStrategyCode;
}

