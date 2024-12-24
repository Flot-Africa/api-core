package africa.flot.infrastructure.service;

import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.client.FineractClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import africa.flot.infrastructure.util.DateUtil;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class LoanServiceImpl implements LoanService {
    private static final Logger LOG = Logger.getLogger(LoanServiceImpl.class);

    @Inject
    @RestClient
    FineractClient fineractClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<Response> getLoanProduct(Integer productId) {
        return fineractClient.getLoanProduct(productId);
    }

    @Override
    public Uni<Response> getClientByExternalId(String externalId) {
        return fineractClient.getClientByExternalId(externalId);
    }

    @Override
    public Uni<Response> createLoan(Integer clientId, Integer productId, BigDecimal amount) {
        return getLoanProduct(productId)
                .onItem().transform(Unchecked.function(response -> {
                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        LOG.error("Failed to fetch loan product: " + response.getStatus());
                        throw new BusinessException("Failed to fetch loan product");
                    }
                    return response.readEntity(JsonNode.class);
                }))
                .onItem().transform(loanProduct -> {
                    try {
                        return createLoanRequest(clientId, productId, amount, loanProduct);
                    } catch (Exception e) {
                        LOG.error("Error creating loan request", e);
                        throw new BusinessException("Error creating loan request: " + e.getMessage());
                    }
                })
                .onItem().transformToUni(requestBody -> fineractClient.createLoan(requestBody));
    }

    private String createLoanRequest(Integer clientId, Integer productId, BigDecimal amount, JsonNode loanProduct) {
        try {
            Map<String, Object> request = new HashMap<>();

            // Validation du montant
            BigDecimal maxPrincipal = new BigDecimal(loanProduct.get("maxPrincipal").asText());
            BigDecimal minPrincipal = new BigDecimal(loanProduct.get("minPrincipal").asText());
            if (amount.compareTo(maxPrincipal) > 0 || amount.compareTo(minPrincipal) < 0) {
                throw new BusinessException("Le montant du prêt doit être entre " + minPrincipal + " et " + maxPrincipal);
            }

            // Configuration de base du prêt
            request.put("clientId", clientId);
            request.put("productId", productId);
            request.put("principal", amount);
            request.put("loanType", "individual");
            request.put("dateFormat", DateUtil.getDateFormat());
            request.put("locale", DateUtil.getLocale());

            // Dates
            String currentDate = DateUtil.formatCurrentDate();
            request.put("submittedOnDate", currentDate);
            request.put("expectedDisbursementDate", currentDate);
            request.put("repaymentsStartingFromDate", currentDate);

            // Configuration du prêt
            request.put("numberOfRepayments", loanProduct.get("numberOfRepayments").asInt());
            request.put("repaymentEvery", loanProduct.get("repaymentEvery").asInt());
            request.put("repaymentFrequencyType", 0); // Jours
            request.put("loanTermFrequency", loanProduct.get("numberOfRepayments").asInt());
            request.put("loanTermFrequencyType", 0); // Jours

            // Configuration des intérêts
            request.put("interestRatePerPeriod", loanProduct.get("interestRatePerPeriod").asDouble());
            request.put("interestType", 1); // Flat
            request.put("interestCalculationPeriodType", 1);
            request.put("amortizationType", 1); // Equal installments
            request.put("interestRateFrequencyType", 3); // Par an

            // Stratégie et paramètres
            request.put("transactionProcessingStrategyCode", "principal-interest-penalties-fees-order-strategy");
            request.put("loanScheduleProcessingType", "HORIZONTAL");
            request.put("daysInYearType", 1);
            request.put("enableInstallmentLevelDelinquency", false);

            // Périodes de grâce
            request.put("graceOnPrincipalPayment", 1);
            request.put("graceOnInterestPayment", 1);
            request.put("graceOnInterestCharged", 1);
            request.put("graceOnArrearsAgeing", 1);

            // Données de décaissement
            Map<String, Object> disbursement = new HashMap<>();
            disbursement.put("expectedDisbursementDate", currentDate);
            disbursement.put("principal", amount);
            request.put("disbursementData", List.of(disbursement));

            request.put("maxOutstandingLoanBalance", maxPrincipal);

            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            LOG.error("Error creating loan request", e);
            throw new BusinessException("Error creating loan request: " + e.getMessage());
        }
    }
}