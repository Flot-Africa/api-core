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

    @Inject
    @RestClient
    FineractClient fineractClient;

    @Inject
    ObjectMapper objectMapper;


    @Inject
    Vertx vertx;

    private Executor vertxExecutor;

    /**
     * Au démarrage, on construit l’Executor qui exécute la tâche
     * sur le Context Vert.x (l'event loop).
     */
    @PostConstruct
    void init() {
        vertxExecutor = command ->
                // runOnContext(...) exécute la commande sur l’event loop
                vertx.getOrCreateContext().runOnContext(command);
    }

    /**
     * Récupère un produit de prêt via Fineract.
     * Si l'appel est bloquant, on le déplace sur un WorkerPool,
     * puis on revient sur l'event loop.
     */
    @Override
    public Uni<Response> getLoanProduct(Integer productId) {
        return Uni.createFrom().item(productId)
                // Bascule sur WorkerPool pour appeler Fineract (bloquant)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(fineractClient::getLoanProduct)
                // Reviens sur l'event loop
                .emitOn(vertxExecutor);
    }

    /**
     * Récupère un client (et ses comptes) via un externalId.
     * Même logique que pour getLoanProduct(...).
     */
    @Override
    public Uni<Response> getClientByExternalId(String externalId) {
        return Uni.createFrom().item(externalId)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(fineractClient::getClientByExternalId)
                .emitOn(vertxExecutor);
    }

    /**
     * Crée un prêt pour le client dans Fineract :
     * 1) Appel getLoanProduct(...) sur WorkerPool
     * 2) Reviens event loop
     * 3) Construit la requête JSON
     * 4) Appel createLoan(...) sur WorkerPool
     * 5) Reviens event loop
     */
    @Override
    public Uni<Response> createLoan(Integer clientId, Integer productId, BigDecimal amount, String externalId) {
        // 1) Bascule sur WorkerPool pour récupérer le produit de prêt
        return Uni.createFrom().item(productId)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .flatMap(fineractClient::getLoanProduct)

                // 2) Reviens sur l'event loop
                .emitOn(vertxExecutor)

                // 3) Analyse la réponse, construit la requête JSON
                .onItem().transform(Unchecked.function(response -> {
                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        LOG.error("Failed to fetch loan product: " + response.getStatus());
                        throw new BusinessException("Failed to fetch loan product");
                    }
                    JsonNode loanProduct = response.readEntity(JsonNode.class);
                    return createLoanRequest(clientId, productId, amount, loanProduct, externalId);
                }))

                // 4) Repars sur WorkerPool pour appeler createLoan (bloquant)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(fineractClient::createLoan)

                // 5) Reviens sur event loop
                .emitOn(vertxExecutor);
    }

    /**
     * Construit l'objet JSON (sous forme de String) pour la création du prêt.
     * Ici, pas d'accès BD, donc on peut rester n'importe où,
     * mais on le fait plutôt sur l'event loop ou WorkerPool selon la logique.
     */
    private String createLoanRequest(Integer clientId, Integer productId, BigDecimal amount, JsonNode loanProduct, String externalId) {
        try {
            Map<String, Object> request = new HashMap<>();

            // Configuration de base
            request.put("clientId", clientId);
            request.put("productId", productId);
            request.put("principal", amount);  // Montant égal au coût du véhicule
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

            // Périodes de grâce (mises à 0 pour commencer les paiements immédiatement)
            request.put("graceOnPrincipalPayment", 0);
            request.put("graceOnInterestPayment", 0);
            request.put("graceOnInterestCharged", 0);
            request.put("graceOnArrearsAgeing", 0);

            // Pas de frais car TAEG = 0
            request.put("charges", List.of());

            return objectMapper.writeValueAsString(request);

        } catch (Exception e) {
            LOG.error("Erreur lors de la création de la requête de prêt", e);
            throw new BusinessException("Erreur lors de la création de la requête de prêt: " + e.getMessage());
        }
    }
}
