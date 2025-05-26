package africa.flot.application.service;

import africa.flot.application.config.Hub2Config;
import africa.flot.application.dto.command.MobileMoneyPaymentCommand;
import africa.flot.application.dto.command.ProcessPaymentCommand;
import africa.flot.domain.model.FlotLoan;
import africa.flot.domain.model.enums.TransactionStatus;
import africa.flot.infrastructure.client.Hub2ApiClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class Hub2PaymentService {

    private static final Logger LOG = Logger.getLogger(Hub2PaymentService.class);

    @Inject
    @RestClient
    Hub2ApiClient hub2Client;

    @Inject
    Hub2Config hub2Config;

    @Inject
    FlotLoanService flotLoanService;

    @Inject
    LeadPaymentIntentService leadPaymentIntentService;

    /**
     * Initie un paiement Mobile Money pour un prêt
     */
    @WithTransaction
    public Uni<MobileMoneyPaymentCommand> initiatePayment(MobileMoneyPaymentCommand command) {
        LOG.infof("Initiation du paiement mobile money pour le prêt %s, montant: %.2f, provider: %s",
                command.getLoanId(), command.getAmount(), command.getProvider());

        return FlotLoan.<FlotLoan>findById(command.getLoanId())
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Prêt introuvable: " + command.getLoanId()))
                .flatMap(loan -> {
                    // 1. Obtenir ou créer un PaymentIntent
                    command.setLeadId(loan.getLeadId());

                    return leadPaymentIntentService.getOrCreateActiveIntent(
                            loan.getLeadId(), loan.getId(), command.getAmount()
                    ).flatMap(intent -> {
                        // Enregistrer les informations du PaymentIntent
                        LOG.infof("Utilisation du PaymentIntent: %s", intent.getHub2IntentId());
                        command.setPaymentIntentId(intent.getHub2IntentId());
                        command.setPaymentIntentToken(intent.getHub2Token());
                        command.setExternalReference("loan_" + loan.getId() + "_" + System.currentTimeMillis());

                        // Mettre à jour les préférences de paiement
                        return leadPaymentIntentService.updatePaymentPreferences(
                                intent.getHub2IntentId(), command.getProvider(), command.getPhoneNumber()
                        ).flatMap(updatedIntent -> {
                            // 2. Initier le paiement mobile money
                            Map<String, Object> paymentPayload = createMobileMoneyPayload(command);

                            return hub2Client.initiatePayment(
                                    hub2Config.getApiKey(),
                                    hub2Config.getMerchantId(),
                                    hub2Config.getEnvironment(),
                                    command.getPaymentIntentId(),
                                    paymentPayload
                            );
                        });
                    }).flatMap(payment -> {
                        String status = (String) payment.get("status");
                        LOG.infof("Paiement initié: %s, statut: %s", payment.get("id"), status);

                        // Mettre à jour le statut de l'intent
                        return leadPaymentIntentService.updateIntentStatus(command.getPaymentIntentId(), status)
                                .map(updatedIntent -> {
                                    // 3. Mettre à jour la commande avec le statut
                                    TransactionStatus transactionStatus = parseTransactionStatus(status);
                                    command.setTransactionStatus(transactionStatus);

                                    if (transactionStatus == TransactionStatus.COMPLETED) {
                                        // Si le paiement est immédiatement complété, programmer le traitement du paiement
                                        ProcessPaymentCommand processCommand = createProcessPaymentCommand(command);
                                        flotLoanService.processPayment(processCommand)
                                                .subscribe().with(
                                                        payment1 -> LOG.info("Paiement traité avec succès"),
                                                        error -> LOG.error("Erreur lors du traitement du paiement", error)
                                                );
                                    }

                                    return command;
                                });
                    });
                });
    }

    /**
     * Vérifie le statut d'un paiement HUB2 en cours
     */
    public Uni<Map<String, Object>> checkPaymentStatus(String paymentIntentId, String token) {
        LOG.infof("Vérification du statut du paiement %s", paymentIntentId);

        return hub2Client.getPaymentIntent(
                hub2Config.getApiKey(),
                hub2Config.getMerchantId(),
                hub2Config.getEnvironment(),
                paymentIntentId,
                token
        ).flatMap(response -> {
            String status = (String) response.get("status");
            LOG.infof("Statut du paiement %s: %s", paymentIntentId, status);

            // Mettre à jour le statut dans notre base de données
            return leadPaymentIntentService.updateIntentStatus(paymentIntentId, status)
                    .map(updatedIntent -> response);
        });
    }

    /**
     * Complète un paiement qui nécessite une authentification (OTP)
     */
    @WithTransaction
    public Uni<MobileMoneyPaymentCommand> completePayment(MobileMoneyPaymentCommand command) {
        if (command.getOtp() == null || command.getOtp().isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("Code OTP obligatoire"));
        }

        LOG.infof("Complétion du paiement %s avec OTP", command.getPaymentIntentId());

        Map<String, Object> authPayload = new HashMap<>();
        authPayload.put("token", command.getPaymentIntentToken());
        authPayload.put("confirmationCode", command.getOtp());

        return hub2Client.completeAuthentication(
                hub2Config.getApiKey(),
                hub2Config.getMerchantId(),
                hub2Config.getEnvironment(),
                command.getPaymentIntentId(),
                authPayload
        ).flatMap(result -> {
            String status = (String) result.get("status");
            LOG.infof("Authentification terminée: statut %s", status);

            return leadPaymentIntentService.updateIntentStatus(command.getPaymentIntentId(), status)
                    .map(updatedIntent -> {
                        command.setTransactionStatus(parseTransactionStatus(status));

                        if (command.getTransactionStatus() == TransactionStatus.COMPLETED) {
                            // Finaliser le paiement si l'authentification a réussi
                            ProcessPaymentCommand processCommand = createProcessPaymentCommand(command);
                            flotLoanService.processPayment(processCommand)
                                    .subscribe().with(
                                            payment -> LOG.info("Paiement traité avec succès après OTP"),
                                            error -> LOG.error("Erreur lors du traitement du paiement après OTP", error)
                                    );
                        }

                        return command;
                    });
        });
    }

    /**
     * Traite une notification de webhook de paiement
     */
    @WithTransaction
    public Uni<Void> processPaymentWebhook(Map<String, Object> payload) {
        String event = (String) payload.get("event");
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        String status = (String) data.get("status");
        String paymentIntentId = (String) data.get("id");

        LOG.infof("Traitement webhook: %s, statut: %s, id: %s", event, status, paymentIntentId);

        return leadPaymentIntentService.updateIntentStatus(paymentIntentId, status)
                .flatMap(intent -> {
                    // Pour les paiements réussis, traiter le paiement
                    if (("payment.succeeded".equals(event) || "payment_intent.succeeded".equals(event))
                            && "succeeded".equals(status)) {

                        return FlotLoan.<FlotLoan>findById(intent.getLoanId())
                                .flatMap(loan -> {
                                    ProcessPaymentCommand command = new ProcessPaymentCommand();
                                    command.setLoanId(intent.getLoanId());
                                    command.setAmount(intent.getAmount().doubleValue());
                                    command.setPaymentMethod(africa.flot.domain.model.enums.PaymentMethod.MOBILE_MONEY);
                                    command.setExternalReference(paymentIntentId);
                                    command.setNotes("Paiement HUB2 Mobile Money via webhook - " + intent.getProvider());
                                    command.setCreatedBy("HUB2_WEBHOOK");

                                    return flotLoanService.processPayment(command);
                                }).replaceWithVoid();
                    }

                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Crée le payload pour un paiement mobile money
     */
    private Map<String, Object> createMobileMoneyPayload(MobileMoneyPaymentCommand command) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", command.getPaymentIntentToken());
        payload.put("paymentMethod", "mobile_money");
        payload.put("country", "CI"); // Code pays Côte d'Ivoire
        payload.put("provider", command.getProvider().toLowerCase());

        Map<String, Object> mobileMoneyDetails = new HashMap<>();
        mobileMoneyDetails.put("msisdn", command.getPhoneNumber());
        if (command.getOtp() != null && !command.getOtp().isBlank()) {
            mobileMoneyDetails.put("otp", command.getOtp());
        }

        payload.put("mobileMoney", mobileMoneyDetails);
        return payload;
    }

    /**
     * Convertit un statut HUB2 en TransactionStatus interne
     */
    private TransactionStatus parseTransactionStatus(String hub2Status) {
        if (hub2Status == null) return TransactionStatus.INITIATED;

        return switch (hub2Status) {
            case "payment_required" -> TransactionStatus.INITIATED;
            case "pending" -> TransactionStatus.PENDING;
            case "processing" -> TransactionStatus.PENDING;
            case "action_required" -> TransactionStatus.PENDING; // Nécessite OTP
            case "succeeded" -> TransactionStatus.COMPLETED;
            case "failed" -> TransactionStatus.FAILED;
            default -> TransactionStatus.INITIATED;
        };
    }


    /**
     * Crée une commande ProcessPaymentCommand à partir d'un MobileMoneyPaymentCommand
     */
    private ProcessPaymentCommand createProcessPaymentCommand(MobileMoneyPaymentCommand command) {
        ProcessPaymentCommand processCommand = new ProcessPaymentCommand();
        processCommand.setLoanId(command.getLoanId());
        processCommand.setAmount(command.getAmount().doubleValue());
        processCommand.setPaymentMethod(command.getPaymentMethod());
        processCommand.setExternalReference(command.getExternalReference());
        processCommand.setNotes("Paiement Mobile Money via HUB2 - " + command.getProvider());
        processCommand.setCreatedBy(command.getCreatedBy() != null ? command.getCreatedBy() : "HUB2_SERVICE");
        return processCommand;
    }
}