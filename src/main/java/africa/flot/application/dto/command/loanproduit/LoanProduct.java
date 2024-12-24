package africa.flot.application.dto.command.loanproduit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class LoanProduct {

    @NotNull(message = "accountMovesOutOfNPAOnlyOnArrearsCompletion est obligatoire")
    private Boolean accountMovesOutOfNPAOnlyOnArrearsCompletion;

    @NotNull(message = "accountingRule est obligatoire")
    private Integer accountingRule;

    @Valid
    private LoanAllowAttributeOverrides allowAttributeOverrides;

    private Boolean allowCompoundingOnEod;
    private Boolean allowVariableInstallments;

    @NotNull(message = "amortizationType est obligatoire")
    private Integer amortizationType;

    @Valid @Size(min = 1, message = "Au moins un mapping charge off nécessaire")
    private List<ChargeOffMapping> chargeOffReasonsToExpenseMappings;

    @Valid
    private List<Charge> charges;

    @Pattern(regexp = "\\d{2} [A-Za-z]+ \\d{4}", message = "Format de date invalide")
    private String closeDate;

    @Valid
    private List<CreditAllocation> creditAllocation;

    @NotBlank(message = "currencyCode est obligatoire")
    @Pattern(regexp = "EUR", message = "Seul EUR est accepté")
    private String currencyCode;

    private String description;

    @Valid
    private List<FeeIncomeMapping> feeToIncomeAccountMappings;

    @NotNull(message = "interestRatePerPeriod est obligatoire")
    @DecimalMin(value = "0.0")
    private Double interestRatePerPeriod;

    @NotBlank(message = "name est obligatoire")
    @Size(min = 3, max = 100)
    private String name;

    @NotBlank(message = "shortName est obligatoire")
    @Size(min = 2, max = 10)
    private String shortName;

    @Valid
    private List<PaymentAllocation> paymentAllocation;

    @Valid
    private List<PaymentChannelMapping> paymentChannelToFundSourceMappings;
}