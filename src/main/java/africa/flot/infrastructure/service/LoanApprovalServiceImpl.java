package africa.flot.infrastructure.service;

import africa.flot.application.ports.LoanApprovalService;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.infrastructure.client.FineractClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@ApplicationScoped
public class LoanApprovalServiceImpl implements LoanApprovalService {

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
    public Uni<Response> approveLoan(String loanExternalId) {
        BUSINESS_LOG.debugf("Début du processus d'approbation pour le prêt: {}", loanExternalId);
        AUDIT_LOG.infof("Tentative d'approbation du prêt - ExternalId: {}", loanExternalId);

        return fineractClient.getLoanTemplate(loanExternalId, "approval")
                .onItem().transform(Unchecked.function(templateResponse -> {
                    if (templateResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                        errorf_LOG.errorf("Échec de la récupération du template: {}", templateResponse.getStatus());
                        throw new BusinessException("Échec de la récupération du template: " + templateResponse.getStatus());
                    }

                    try {
                        JsonNode templateData = templateResponse.readEntity(JsonNode.class);
                        BUSINESS_LOG.debugf("Template reçu pour le prêt {}", loanExternalId);

                        JsonObject approvalBody = buildApprovalRequestBody(templateData);
                        BUSINESS_LOG.debugf("Corps de la requête d'approbation construit: {}", approvalBody.encode());

                        return approvalBody;
                    } catch (Exception e) {
                        errorf_LOG.errorf("Erreur lors du traitement du template: {}", e.getMessage());
                        throw new BusinessException("Erreur lors du traitement du template: " + e.getMessage());
                    }
                }))
                .flatMap(approvalBody ->
                        fineractClient.postLoanCommand(loanExternalId, "approve", approvalBody.encode())
                )
                .onItem().invoke(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        BUSINESS_LOG.infof("Prêt approuvé avec succès: {}", loanExternalId);
                        AUDIT_LOG.infof("Prêt approuvé - ExternalId: {}", loanExternalId);
                    } else {
                        errorf_LOG.errorf("Échec de l'approbation - Code: {}", response.getStatus());
                    }
                })
                .flatMap(Unchecked.function(approvalResponse -> {
                    if (approvalResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                        return Uni.createFrom().item(approvalResponse);
                    }

                    try {
                        BUSINESS_LOG.debugf("Préparation du décaissement pour le prêt: {}", loanExternalId);
                        JsonObject disbursementBody = buildDisbursementRequestBody(approvalResponse, loanExternalId);
                        AUDIT_LOG.infof("Tentative de décaissement du prêt - ExternalId: {}", loanExternalId);
                        return fineractClient.postLoanCommand(loanExternalId, "disburse", disbursementBody.encode());
                    } catch (Exception e) {
                        errorf_LOG.errorf("Erreur lors de la préparation du décaissement: {}", e.getMessage());
                        throw new BusinessException("Erreur lors de la préparation du décaissement: " + e.getMessage());
                    }
                }))
                .onItem().invoke(finalResponse -> {
                    if (finalResponse.getStatus() == Response.Status.OK.getStatusCode()) {
                        BUSINESS_LOG.infof("Prêt décaissé avec succès: {}", loanExternalId);
                        AUDIT_LOG.infof("Prêt décaissé - ExternalId: {}", loanExternalId);
                    } else {
                        errorf_LOG.errorf("Échec du décaissement - Code: {} - ExternalId: {}",
                                finalResponse.getStatus(), loanExternalId);
                    }
                })
                .onFailure().invoke(e ->
                        errorf_LOG.errorf("Erreur lors du processus d'approbation/décaissement - ExternalId: {} - Message: {}",
                                loanExternalId, e.getMessage())
                );
    }

    private JsonObject buildApprovalRequestBody(JsonNode templateData) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

        BigDecimal approvalAmount = extractApprovalAmount(templateData);
        BUSINESS_LOG.debugf("Montant d'approbation calculé: {}", approvalAmount);

        return new JsonObject()
                .put("approvedOnDate", currentDate.format(formatter))
                .put("expectedDisbursementDate", currentDate.format(formatter))
                .put("approvedLoanAmount", approvalAmount)
                .put("note", "")
                .put("dateFormat", "dd MMMM yyyy")
                .put("locale", "fr");
    }

    private JsonObject buildDisbursementRequestBody(Response approvalResponse, String loanExternalId) {
        try {
            JsonNode responseData = objectMapper.readTree(approvalResponse.readEntity(String.class));
            JsonNode changes = responseData.path("changes");

            BigDecimal transactionAmount = new BigDecimal(changes.path("netDisbursalAmount").asText("21600000"));
            String resourceExternalId = responseData.path("resourceExternalId").asText(loanExternalId);

            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

            BUSINESS_LOG.debugf("Construction du corps de la requête de décaissement - Montant: {} - ExternalId: {}",
                    transactionAmount, resourceExternalId);

            return new JsonObject()
                    .put("actualDisbursementDate", currentDate.format(formatter))
                    .put("transactionAmount", transactionAmount)
                    .put("externalId", resourceExternalId)
                    .put("paymentTypeId", 1)
                    .put("note", "")
                    .put("dateFormat", "dd MMMM yyyy")
                    .put("locale", "fr");
        } catch (Exception e) {
            errorf_LOG.errorf("Erreur lors de la construction de la requête de décaissement: {}", e.getMessage());
            throw new BusinessException("Erreur lors de la construction de la requête de décaissement: " + e.getMessage());
        }
    }

    private BigDecimal extractApprovalAmount(JsonNode templateData) {
        try {
            if (templateData.has("approvalAmount")) {
                BigDecimal amount = new BigDecimal(templateData.get("approvalAmount").asText());
                BUSINESS_LOG.debugf("Montant d'approbation extrait du template: {}", amount);
                return amount;
            } else if (templateData.has("principal")) {
                BigDecimal amount = new BigDecimal(templateData.get("principal").asText());
                BUSINESS_LOG.debugf("Montant principal extrait du template: {}", amount);
                return amount;
            } else {
                BUSINESS_LOG.warnf("Aucun montant trouvé dans le template, utilisation du montant par défaut");
                return new BigDecimal("21600000.00");
            }
        } catch (Exception e) {
            errorf_LOG.errorf("Erreur lors de l'extraction du montant d'approbation: {}", e.getMessage());
            return new BigDecimal("21600000.00");
        }
    }
}