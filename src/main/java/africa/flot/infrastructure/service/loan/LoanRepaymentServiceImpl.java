package africa.flot.infrastructure.service.loan;

import africa.flot.domain.service.LoanRepaymentService;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.infrastructure.client.FineractClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
@Deprecated(since = "1.0.0", forRemoval = true)
public class LoanRepaymentServiceImpl implements LoanRepaymentService {

    private static final Logger LOG = Logger.getLogger(LoanServiceImpl.class);
    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger errorf_LOG = Logger.getLogger("errorf");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    @RestClient
    FineractClient fineractClient;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<Response> makeRepayment(String loanExternalId, BigDecimal amount) {
        BUSINESS_LOG.debugf("Starting repayment process - Loan: {} - Amount: {}",
                loanExternalId, amount != null ? amount : "not specified");
        AUDIT_LOG.infof("Repayment attempt - LoanId: {}", loanExternalId);

        return fineractClient.getLoanTransactionTemplate(loanExternalId, "repayment")
                .onItem().transform(Unchecked.function(templateResponse -> {
                    if (templateResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                        errorf_LOG.errorf("Failed to retrieve repayment template: {}",
                                templateResponse.getStatus());
                        throw new BusinessException("Failed to retrieve repayment template: "
                                + templateResponse.getStatus());
                    }

                    try {
                        JsonNode templateData = templateResponse.readEntity(JsonNode.class);
                        BUSINESS_LOG.debugf("Repayment template received for loan {}", loanExternalId);

                        BigDecimal repaymentAmount = determineRepaymentAmount(templateData, amount);
                        JsonObject repaymentBody = buildRepaymentRequestBody(loanExternalId, repaymentAmount);

                        BUSINESS_LOG.debugf("Repayment request body built: {}",
                                repaymentBody.encode());

                        return repaymentBody;
                    } catch (Exception e) {
                        errorf_LOG.errorf("Error processing template: {}", e.getMessage());
                        throw new BusinessException("Error processing template: " + e.getMessage());
                    }
                }))
                .flatMap(repaymentBody ->
                        fineractClient.postLoanTransaction(loanExternalId, "repayment", repaymentBody.encode())
                )
                .onItem().invoke(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        BUSINESS_LOG.infof("Repayment successfully processed - Loan: {}", loanExternalId);
                        AUDIT_LOG.infof("Repayment successful - LoanId: {}", loanExternalId);
                    } else {
                        errorf_LOG.errorf("Repayment failed - Code: {} - Loan: {}",
                                response.getStatus(), loanExternalId);
                    }
                })
                .onFailure().invoke(e ->
                        errorf_LOG.errorf("Error in repayment process - LoanId: {} - Message: {}",
                                loanExternalId, e.getMessage())
                );
    }

    @Override
    public Uni<Response> getNextRepaymentDetails(String loanExternalId) {
        BUSINESS_LOG.debugf("Retrieving next repayment details - Loan: {}", loanExternalId);

        return fineractClient.getLoanTransactionTemplate(loanExternalId, "repayment")
                .onItem().invoke(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        BUSINESS_LOG.infof("Next repayment details retrieved - Loan: {}", loanExternalId);
                    } else {
                        errorf_LOG.errorf("Failed to retrieve details - Code: {} - Loan: {}",
                                response.getStatus(), loanExternalId);
                    }
                });
    }

    private BigDecimal determineRepaymentAmount(JsonNode templateData, BigDecimal requestedAmount) {
        try {
            BigDecimal templateAmount = new BigDecimal(templateData.path("amount").asText("0"));

            if (requestedAmount == null) {
                BUSINESS_LOG.debugf("Using suggested template amount: {}", templateAmount);
                return templateAmount;
            }

            if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Repayment amount must be greater than zero");
            }

            BUSINESS_LOG.debugf("Using requested amount: {} (template suggested: {})",
                    requestedAmount, templateAmount);
            return requestedAmount;

        } catch (Exception e) {
            errorf_LOG.errorf("Error determining amount: {}", e.getMessage());
            throw new BusinessException("Error determining amount: " + e.getMessage());
        }
    }

    private JsonObject buildRepaymentRequestBody(String loanExternalId, BigDecimal amount) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

        // Générer un externalId unique en combinant l'ID du prêt avec un UUID court
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        String transactionExternalId = loanExternalId + "-" + shortUuid;

        return new JsonObject()
                .put("transactionDate", currentDate.format(formatter))
                .put("transactionAmount", amount)
                .put("externalId", transactionExternalId)
                .put("paymentTypeId", 1)
                .put("note", "")
                .put("dateFormat", "dd MMMM yyyy")
                .put("locale", "fr");
    }
}