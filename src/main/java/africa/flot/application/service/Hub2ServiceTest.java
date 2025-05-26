package africa.flot.application.service;

import africa.flot.application.config.Hub2Config;
import africa.flot.application.dto.request.PayementIntentRequest;
import africa.flot.application.dto.request.PaymentIntentResponse;
import africa.flot.infrastructure.client.Hub2ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.HashMap;

@ApplicationScoped
public class Hub2ServiceTest {

    private static final Logger LOG = Logger.getLogger(Hub2ServiceTest.class);
    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    @RestClient
    Hub2ApiClient hub2Client;

    @Inject
    Hub2Config hub2Config;

    @Inject
    ObjectMapper objectMapper;

    @WithTransaction
    public Uni<PaymentIntentResponse> repayLoanTest(PayementIntentRequest request) {
        LOG.infof("Début de repayLoanTest - Montant: %s %s", request.getAmount(), request.getCurrency());

        // Vérification des dépendances
        if (hub2Client == null) {
            LOG.error("hub2Client est null - erreur d'injection");
            return Uni.createFrom().failure(new RuntimeException("hub2Client non injecté"));
        }

        if (hub2Config == null) {
            LOG.error("hub2Config est null - erreur d'injection");
            return Uni.createFrom().failure(new RuntimeException("hub2Config non injecté"));
        }

        // Récupération des valeurs de configuration Hub2
        String apiKey = hub2Config.getApiKey();
        String merchantId = hub2Config.getMerchantId();
        String environment = hub2Config.getEnvironment();

        LOG.infof("Configuration Hub2: apiKey=%s, merchantId=%s, environment=%s",
                apiKey != null ? "présent" : "null",
                merchantId != null ? "présent" : "null",
                environment != null ? environment : "null");

        try {
            // Conversion de PayementIntentRequest en Map<String, Object>
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(request, HashMap.class);

            LOG.infof("Appel de createPaymentIntent avec payload: %s", payload);

            return hub2Client.createPaymentIntent(apiKey, merchantId, environment, payload)
                    .map(response -> {
                        LOG.infof("Réponse reçue de Hub2: %s", response);
                        try {
                            PaymentIntentResponse paymentIntent = objectMapper.convertValue(response, PaymentIntentResponse.class);
                            AUDIT_LOG.infof("Payment intent créé - ID: %s", paymentIntent.getId());
                            return paymentIntent;
                        } catch (Exception ex) {
                            LOG.error("Erreur lors de la conversion de la réponse", ex);
                            throw new RuntimeException("Erreur lors de la conversion de la réponse", ex);
                        }
                    })
                    .onFailure().invoke(throwable ->
                            LOG.error("Erreur création payment intent", throwable)
                    );
        } catch (Exception e) {
            LOG.error("Erreur lors de la préparation de la requête", e);
            return Uni.createFrom().failure(new RuntimeException("Erreur lors de la préparation de la requête", e));
        }
    }
}