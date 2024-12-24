package africa.flot.domain.model;

import africa.flot.domain.model.enums.LoanStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan extends PanacheEntityBase {

    @Id
    private Integer productId;
    private Integer clientId;
    private Long principal;
    private Integer loanTermFrequency;
    private Integer numberOfRepayments;
    private Integer repaymentEvery;
    private Double interestRatePerPeriod;

    private LocalDate expectedDisbursementDate;
    private LocalDate repaymentsStartingFromDate;
    private LocalDate submittedOnDate;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    // Paramètres techniques
    private String dateFormat;
    private String locale;
    private Integer daysInYearType;
    private Integer amortizationType;
    private Integer interestType;
    private Integer interestCalculationPeriodType;
    private Integer interestRateFrequencyType;
    private String loanScheduleProcessingType;
    private String transactionProcessingStrategyCode;

    // Périodes de grâce
    private Integer graceOnArrearsAgeing;
    private Integer graceOnInterestCharged;
    private Integer graceOnInterestPayment;
    private Integer graceOnPrincipalPayment;

    // Configuration du prêt
    private String loanType;
    private Long maxOutstandingLoanBalance;
    private Boolean enableInstallmentLevelDelinquency;


}