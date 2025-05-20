package africa.flot.application.service;

import africa.flot.application.config.Hub2Config;
import africa.flot.domain.model.LeadPaymentIntent;
import africa.flot.infrastructure.client.Hub2ApiClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LeadPaymentIntentService {

    private static final Logger LOG = Logger.getLogger(LeadPaymentIntentService.class);

    @Inject
    @RestClient
    Hub2ApiClient hub2Client;

    @Inject
    Hub2Config hub2Config;

    /**
     * Récupère ou crée un PaymentIntent actif pour un lead et un montant spécifique
     */
    @WithTransaction
    public Uni<LeadPaymentIntent> getOrCreateActiveIntent(UUID leadId, UUID loanId, BigDecimal amount) {
        LOG.infof("Recherche d'un intent actif pour lead %s, loan %s, montant %.2f",
                leadId, loanId, amount);

        return LeadPaymentIntent.<LeadPaymentIntent>find(
                        "leadId = ?1 AND loanId = ?2 AND active = true AND status NOT IN ('succeeded', 'failed')",
                        leadId, loanId)
                .firstResult()
                .chain(existingIntent -> {
                    if (existingIntent != null) {
                        LOG.info("Intent existant trouvé: " + existingIntent.getHub2IntentId());
                        // Vérifier si l'amount correspond
                        if (existingIntent.getAmount().compareTo(amount) == 0) {
                            // Même montant, réutiliser l'intent
                            return Uni.createFrom().item(existingIntent);
                        } else {
                            LOG.infof("Montant différent (%.2f vs %.2f), création d'un nouvel intent",
                                    existingIntent.getAmount(), amount);
                            // Montant différent, désactiver l'ancien intent et créer un nouveau
                            existingIntent.setActive(false);
                            return existingIntent.persistAndFlush()
                                    .chain(() -> createNewPaymentIntent(leadId, loanId, amount));
                        }
                    } else {
                        LOG.info("Aucun intent actif trouvé, création d'un nouveau");
                        // Pas d'intent actif, en créer un nouveau
                        return createNewPaymentIntent(leadId, loanId, amount);
                    }
                });
    }

    /**
     * Met à jour les préférences de paiement et le statut d'un intent
     */
    @WithTransaction
    public Uni<LeadPaymentIntent> updatePaymentPreferences(String hub2IntentId, String provider, String phoneNumber) {
        return LeadPaymentIntent.<LeadPaymentIntent>find("hub2IntentId", hub2IntentId)
                .firstResult()
                .chain(intent -> {
                    if (intent == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Intent introuvable: " + hub2IntentId));
                    }

                    intent.setProvider(provider);
                    intent.setPhoneNumber(phoneNumber);
                    intent.setLastAttemptAt(LocalDateTime.now());

                    return intent.<LeadPaymentIntent>persistAndFlush();
                });
    }

    /**
     * Met à jour le statut d'un intent
     */
    @WithTransaction
    public Uni<LeadPaymentIntent> updateIntentStatus(String hub2IntentId, String status) {
        LOG.infof("Mise à jour du statut de l'intent %s: %s", hub2IntentId, status);

        return LeadPaymentIntent.<LeadPaymentIntent>find("hub2IntentId", hub2IntentId)
                .firstResult()
                .chain(intent -> {
                    if (intent == null) {
                        LOG.warn("Intent introuvable: " + hub2IntentId);
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Intent introuvable: " + hub2IntentId));
                    }

                    intent.setStatus(status);
                    intent.setLastAttemptAt(LocalDateTime.now());

                    // Si statut terminal, mettre à jour les champs correspondants
                    if ("succeeded".equals(status) || "failed".equals(status)) {
                        intent.setCompletionDate(LocalDateTime.now());
                        if ("failed".equals(status)) {
                            intent.setActive(false);
                        }
                    }

                    return intent.<LeadPaymentIntent>persistAndFlush();
                });
    }

    /**
     * Crée un nouveau PaymentIntent sur HUB2
     */
    private Uni<LeadPaymentIntent> createNewPaymentIntent(UUID leadId, UUID loanId, BigDecimal amount) {
        LOG.infof("Création d'un nouveau PaymentIntent pour lead %s, loan %s, montant %.2f",
                leadId, loanId, amount);

        // Créer une référence unique
        String reference = "loan_" + loanId + "_" + System.currentTimeMillis();
        Map<String, Object> intentPayload = new HashMap<>();
        intentPayload.put("customerReference", "driver_" + leadId);
        intentPayload.put("purchaseReference", reference);
        intentPayload.put("amount", amount.intValue());
        intentPayload.put("currency", "XOF");

        return hub2Client.createPaymentIntent(
                hub2Config.getApiKey(),
                hub2Config.getMerchantId(),
                hub2Config.getEnvironment(),
                intentPayload
        ).map(intent -> {
            LOG.infof("PaymentIntent créé sur HUB2: %s", intent.get("id"));

            LeadPaymentIntent leadIntent = new LeadPaymentIntent();
            leadIntent.setLeadId(leadId);
            leadIntent.setLoanId(loanId);
            leadIntent.setAmount(amount);
            leadIntent.setCurrency("XOF");
            leadIntent.setHub2IntentId((String) intent.get("id"));
            leadIntent.setHub2Token((String) intent.get("token"));
            leadIntent.setStatus((String) intent.get("status"));
            leadIntent.setDueDate(LocalDate.now().plusDays(7)); // 7 jours pour payer
            leadIntent.setCreatedAt(LocalDateTime.now());
            leadIntent.setLastAttemptAt(LocalDateTime.now());
            return leadIntent;
        }).chain(leadIntent -> leadIntent.<LeadPaymentIntent>persistAndFlush());
    }
}