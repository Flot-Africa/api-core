package africa.flot.infrastructure.service;

import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.client.FineractClient;
import africa.flot.infrastructure.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implémentation du LoanService pour gérer la création de prêts via l'API Fineract.
 * On suppose que FineractClient est potentiellement bloquant,
 * donc on bascule les appels sur un WorkerPool.
 */
@ApplicationScoped
public class LoanServiceImpl implements LoanService {

    private static final Logger LOG = Logger.getLogger(LoanServiceImpl.class);
    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger errorf_LOG = Logger.getLogger("errorf");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    @RestClient
    FineractClient fineractClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Vertx vertx;

    private Executor vertxExecutor;

    @PostConstruct
    void init() {
        vertxExecutor = command -> vertx.getOrCreateContext().runOnContext(command);
        BUSINESS_LOG.infof("LoanService initialisé");
    }

    @Override
    public Uni<Response> getLoanProduct(Integer productId) {
        BUSINESS_LOG.debugf("Récupération du produit de prêt: "+ productId);

        return Uni.createFrom().item(productId)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(fineractClient::getLoanProduct)
                .emitOn(vertxExecutor)
                .onItem().invoke(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        BUSINESS_LOG.infof("Produit de prêt {} récupéré avec succès", productId);
                    } else {
                        errorf_LOG.errorf("Échec de la récupération du produit {}: {}",
                                productId, response.getStatus());
                    }
                });
    }

    @Override
    public Uni<Response> getClientByExternalId(String externalId) {
        BUSINESS_LOG.debugf("Récupération du client: {}", externalId);

        return Uni.createFrom().item(externalId)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(fineractClient::getClientByExternalId)
                .emitOn(vertxExecutor)
                .onItem().invoke(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        BUSINESS_LOG.infof("Client {} récupéré avec succès", externalId);
                        AUDIT_LOG.infof("Accès aux données client - ExternalId: {}", externalId);
                    } else {
                        errorf_LOG.errorf("Échec de la récupération du client {}: {}",
                                externalId, response.getStatus());
                    }
                });
    }

    @Override
    public Uni<JsonObject> getLoanDetailsForMobile(String externalId) {
        BUSINESS_LOG.debugf("Récupération des détails du prêt pour mobile: {}", externalId);

        return fineractClient.getLoanByExternalId(externalId, "all", "guarantors,futureSchedule")
                .onItem().transform(Unchecked.function(response -> {
                    validateResponse(response, "Erreur lors de la récupération des détails du prêt pour l'application mobile.");

                    AUDIT_LOG.infof("Accès aux détails du prêt (mobile) - LoanId: {}", externalId);

                    JsonNode loanData = response.readEntity(JsonNode.class);
                    JsonObject result = extractMobileData(loanData);

                    BUSINESS_LOG.infof("Détails du prêt (mobile) extraits avec succès pour: {}", externalId);
                    return result;
                }))
                .onFailure().invoke(e ->
                        errorf_LOG.errorf("Erreur lors de la récupération des détails du prêt mobile: {}", e.getMessage())
                );
    }

    private JsonObject extractMobileData(JsonNode loanData) {
        BUSINESS_LOG.debugf("Extraction des données mobiles");
        Map<String, Object> mobileData = new HashMap<>();

        try {
            JsonNode repaymentSchedule = loanData.path("repaymentSchedule");
            if (!repaymentSchedule.isMissingNode()) {
                double progress = calculateLoanProgress(repaymentSchedule);
                mobileData.put("loanProgress", progress);
                mobileData.put("amountRemaining", getTextOrDefault(repaymentSchedule, "totalOutstanding", "0"));

                JsonNode periods = repaymentSchedule.path("periods");
                if (!periods.isMissingNode() && periods.isArray() && !periods.isEmpty()) {
                    JsonNode firstPeriod = periods.get(0);
                    String dueDate = formatDueDate(firstPeriod.path("dueDate"));
                    mobileData.put("nextDueDate", dueDate);
                    BUSINESS_LOG.debugf("Prochaine échéance: {}", dueDate);
                } else {
                    mobileData.put("nextDueDate", "N/A");
                    BUSINESS_LOG.warnf("Aucune période de remboursement trouvée");
                }
            }

            mobileData.put("principal", getTextOrDefault(loanData, "principal", "0"));

            JsonNode currency = loanData.path("currency");
            if (!currency.isMissingNode()) {
                mobileData.put("currency", getTextOrDefault(currency, "code", "XOF"));
            } else {
                mobileData.put("currency", "XOF");
                BUSINESS_LOG.debugf("Devise non trouvée, utilisation de XOF par défaut");
            }

            return new JsonObject(mobileData);
        } catch (Exception e) {
            errorf_LOG.errorf("Erreur lors de l'extraction des données mobiles", e);
            throw new BusinessException("Erreur lors du traitement des données du prêt");
        }
    }

    @Override
    public Uni<JsonObject> getLoanDetailsForBackOffice(String externalId) {
        BUSINESS_LOG.debugf("Récupération des détails du prêt pour back-office: {}", externalId);

        return fineractClient.getLoanByExternalId(externalId, "all", "guarantors,futureSchedule")
                .onItem().transform(Unchecked.function(response -> {
                    validateResponse(response, "Erreur lors de la récupération des détails du prêt pour le back-office.");

                    AUDIT_LOG.infof("Accès aux détails du prêt (back-office) - LoanId: {}", externalId);

                    JsonNode loanData = response.readEntity(JsonNode.class);
                    JsonObject result = extractBackOfficeData(loanData);

                    BUSINESS_LOG.infof("Détails du prêt (back-office) extraits avec succès pour: {}", externalId);
                    return result;
                }))
                .onFailure().invoke(e ->
                        errorf_LOG.errorf("Erreur lors de la récupération des détails du prêt back-office: {}", e.getMessage())
                );
    }

    private JsonObject extractBackOfficeData(JsonNode loanData) {
        BUSINESS_LOG.debugf("Extraction des données back-office");
        Map<String, Object> backOfficeData = new HashMap<>();

        try {
            backOfficeData.put("loanId", getTextOrDefault(loanData, "id", "N/A"));
            backOfficeData.put("clientName", getTextOrDefault(loanData, "clientName", "N/A"));

            JsonNode status = loanData.path("status");
            backOfficeData.put("status", !status.isMissingNode() ?
                    getTextOrDefault(status, "value", "N/A") : "N/A");

            JsonNode repaymentSchedule = loanData.path("repaymentSchedule");
            if (!repaymentSchedule.isMissingNode()) {
                backOfficeData.put("totalPrincipalPaid",
                        getTextOrDefault(repaymentSchedule, "totalPrincipalPaid", "0"));
                backOfficeData.put("totalOutstanding",
                        getTextOrDefault(repaymentSchedule, "totalOutstanding", "0"));
                BUSINESS_LOG.debugf("Montant restant dû: {}",
                        getTextOrDefault(repaymentSchedule, "totalOutstanding", "0"));
            }

            JsonNode summary = loanData.path("summary");
            backOfficeData.put("arrears",
                    !summary.isMissingNode() ? getTextOrDefault(summary, "totalOverdue", "0") : "0");

            JsonNode periods = repaymentSchedule.path("periods");
            if (!periods.isMissingNode() && periods.isArray() && !periods.isEmpty()) {
                JsonNode firstPeriod = periods.get(0);
                Map<String, Object> installment = new HashMap<>();
                installment.put("dueDate", formatDueDate(firstPeriod.path("dueDate")));
                installment.put("totalDue", getTextOrDefault(firstPeriod, "totalDueForPeriod", "0"));
                backOfficeData.put("nextInstallment", installment);
            } else {
                backOfficeData.put("nextInstallment",
                        Map.of("dueDate", "N/A", "totalDue", "0"));
                BUSINESS_LOG.warnf("Aucune échéance future trouvée");
            }

            return new JsonObject(backOfficeData);
        } catch (Exception e) {
            errorf_LOG.errorf("Erreur lors de l'extraction des données back-office", e);
            throw new BusinessException("Erreur lors du traitement des données du prêt");
        }
    }

    @Override
    public Uni<List<JsonObject>> getLoanRepaymentHistory(String externalId) {
        BUSINESS_LOG.debugf("Récupération de l'historique des paiements: {}", externalId);

        return fineractClient.getLoanByExternalId(externalId, "all", "guarantors,futureSchedule")
                .onItem().transform(Unchecked.function(response -> {
                    validateResponse(response, "Erreur lors de la récupération de l'historique des paiements.");

                    AUDIT_LOG.infof("Accès à l'historique des paiements - LoanId: {}", externalId);

                    JsonNode loanData = response.readEntity(JsonNode.class);
                    List<JsonObject> result = extractRepaymentHistory(loanData);

                    BUSINESS_LOG.infof("Historique des paiements extrait avec succès pour: {}", externalId);
                    return result;
                }))
                .onFailure().invoke(e ->
                        errorf_LOG.errorf("Erreur lors de la récupération de l'historique des paiements: {}", e.getMessage())
                );
    }

    private List<JsonObject> extractRepaymentHistory(JsonNode loanData) {
        BUSINESS_LOG.debugf("Extraction de l'historique des paiements");
        List<JsonObject> repaymentHistory = new ArrayList<>();

        try {
            JsonNode periods = loanData.path("repaymentSchedule").path("periods");
            if (!periods.isMissingNode() && periods.isArray()) {
                periods.forEach(period -> {
                    Map<String, Object> repayment = new HashMap<>();
                    repayment.put("period", period.path("period").asInt(0));
                    repayment.put("dueDate", formatDueDate(period.path("dueDate")));
                    repayment.put("principalDue", getTextOrDefault(period, "principalDue", "0"));
                    repayment.put("principalPaid", getTextOrDefault(period, "principalPaid", "0"));
                    repayment.put("principalOutstanding", getTextOrDefault(period, "principalOutstanding", "0"));
                    repaymentHistory.add(new JsonObject(repayment));
                });
                BUSINESS_LOG.debugf("Nombre de périodes extraites: {}", repaymentHistory.size());
            } else {
                BUSINESS_LOG.warnf("Aucune période de remboursement trouvée dans l'historique");
            }
            return repaymentHistory;
        } catch (Exception e) {
            errorf_LOG.errorf("Erreur lors de l'extraction de l'historique des paiements", e);
            throw new BusinessException("Erreur lors du traitement de l'historique des paiements");
        }
    }

    @Override
    public Uni<Response> createLoan(Integer clientId, Integer productId, BigDecimal amount, String externalId) {
        BUSINESS_LOG.infof("Création d'un prêt - Client: {}, Produit: {}, Montant: {}",
                clientId, productId, amount);

        return Uni.createFrom().item(productId)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(fineractClient::getLoanProduct)
                .emitOn(vertxExecutor)
                .onItem().transform(Unchecked.function(response -> {
                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        errorf_LOG.errorf("Échec de la récupération du produit de prêt: {}", response.getStatus());
                        throw new BusinessException("Failed to fetch loan product");
                    }
                    JsonNode loanProduct = response.readEntity(JsonNode.class);
                    BUSINESS_LOG.debugf("Produit de prêt récupéré, création de la requête");
                    return createLoanRequest(clientId, productId, amount, loanProduct, externalId);
                }))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(fineractClient::createLoan)
                .emitOn(vertxExecutor)
                .onItem().invoke(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        AUDIT_LOG.infof("Prêt créé avec succès - Client: {}, ExternalId: {}",
                                clientId, externalId);
                        BUSINESS_LOG.infof("Prêt créé avec succès pour le client: {}", clientId);
                    } else {
                        errorf_LOG.errorf("Échec de la création du prêt - Client: {}, Status: {}",
                                clientId, response.getStatus());
                    }
                });
    }

    private String createLoanRequest(Integer clientId, Integer productId, BigDecimal amount,
                                     JsonNode loanProduct, String externalId) {
        try {
            BUSINESS_LOG.debugf("Construction de la requête de prêt pour le client: {}", clientId);
            Map<String, Object> request = new HashMap<>();

            // Configuration de base
            request.put("clientId", clientId);
            request.put("productId", productId);
            request.put("principal", amount);
            request.put("loanType", "individual");
            request.put("dateFormat", "dd MMMM yyyy");
            request.put("locale", "fr");
            request.put("externalId", externalId);

            // Dates importantes
            String currentDate = DateUtil.formatCurrentDate();
            request.put("submittedOnDate", currentDate);
            request.put("expectedDisbursementDate", currentDate);
            request.put("repaymentsStartingFromDate", currentDate);

            // Configuration du prêt pour paiements mensuels
            request.put("numberOfRepayments", 864);     // 36 mois * 24 jours
            request.put("repaymentEvery", 1);           // Chaque jour
            request.put("repaymentFrequencyType", 0);   // 0 = Jours
            request.put("loanTermFrequency", 864);      // 36 mois * 24 jours
            request.put("loanTermFrequencyType", 0);    // 0 = Jours

            // Configuration des intérêts (0%)
            request.put("interestRatePerPeriod", 0);    // Taux à 0%
            request.put("interestType", 0);             // 0 = Declining Balance
            request.put("interestCalculationPeriodType", 1); // 1 = Same as repayment period
            request.put("amortizationType", 1);         // 1 = Equal installments
            request.put("interestRateFrequencyType", 3); // 3 = Par an

            // Données de décaissement
            Map<String, Object> disbursement = new HashMap<>();
            disbursement.put("expectedDisbursementDate", currentDate);
            disbursement.put("principal", amount);
            request.put("disbursementData", List.of(disbursement));

            // Configuration additionnelle
            request.put("transactionProcessingStrategyCode", "principal-interest-penalties-fees-order-strategy");
            request.put("loanScheduleProcessingType", "HORIZONTAL");
            request.put("daysInYearType", 360);         // 360 jours par an
            request.put("enableInstallmentLevelDelinquency", false);
            request.put("maxOutstandingLoanBalance", amount);

            // Périodes de grâce
            request.put("graceOnPrincipalPayment", 0);
            request.put("graceOnInterestPayment", 0);
            request.put("graceOnInterestCharged", 0);
            request.put("graceOnArrearsAgeing", 0);

            // Pas de frais car TAEG = 0
            request.put("charges", List.of());

            String jsonRequest = objectMapper.writeValueAsString(request);
            BUSINESS_LOG.debugf("Requête de prêt construite avec succès pour le client: {}", clientId);
            return jsonRequest;

        } catch (Exception e) {
            errorf_LOG.errorf("Erreur lors de la création de la requête de prêt pour le client: {}", clientId, e);
            throw new BusinessException("Erreur lors de la création de la requête de prêt: " + e.getMessage());
        }
    }

    private void validateResponse(Response response, String errorfMessage) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            errorf_LOG.errorf("{} (Status: {})", errorfMessage, response.getStatus());
            throw new BusinessException(errorfMessage);
        }
    }

    private String formatDueDate(JsonNode dueDateNode) {
        try {
            if (dueDateNode.isArray()) {
                if (dueDateNode.size() >= 3) {
                    int year = dueDateNode.get(0).asInt();
                    int month = dueDateNode.get(1).asInt();
                    int day = dueDateNode.get(2).asInt();
                    return String.format("%d-%02d-%02d", year, month, day);
                }
                BUSINESS_LOG.warnf("Format de date invalide (tableau incomplet): {}", dueDateNode);
            } else if (!dueDateNode.isMissingNode() && dueDateNode.isTextual()) {
                return dueDateNode.asText();
            }
            BUSINESS_LOG.warnf("Format de date non pris en charge: {}", dueDateNode);
        } catch (Exception e) {
            errorf_LOG.warnf("Erreur lors du formatage de la date", e);
        }
        return "N/A";
    }

    private String getTextOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode()) {
            BUSINESS_LOG.debugf("Champ '{}' non trouvé, utilisation de la valeur par défaut: {}",
                    fieldName, defaultValue);
        }
        return !field.isMissingNode() ? field.asText(defaultValue) : defaultValue;
    }

    private double calculateLoanProgress(JsonNode repaymentSchedule) {
        try {
            double totalPaid = repaymentSchedule.path("totalPrincipalPaid").asDouble(0.0);
            double totalExpected = repaymentSchedule.path("totalPrincipalExpected").asDouble(1.0);
            double progress = (totalPaid / Math.max(totalExpected, 1.0)) * 100;
            BUSINESS_LOG.debugf("Progression du prêt calculée: "+ progress);
            return progress;
        } catch (Exception e) {
            errorf_LOG.warnf("Erreur lors du calcul de la progression du prêt", e);
            return 0.0;
        }
    }
}