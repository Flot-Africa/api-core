package africa.flot.infrastructure.startup;

import africa.flot.application.config.Hub2Config;
import africa.flot.infrastructure.client.Hub2ApiClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class Hub2WebhookRegistration {

    private static final Logger LOG = Logger.getLogger(Hub2WebhookRegistration.class);

    @Inject
    @RestClient
    Hub2ApiClient hub2Client;

    @Inject
    Hub2Config hub2Config;

    @ConfigProperty(name = "quarkus.http.host")
    String host;

    @ConfigProperty(name = "quarkus.http.port")
    Integer port;

    // Rendre ces propriétés optionnelles
    private Optional<Integer> getSslPort() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.http.ssl-port", Integer.class);
    }

    private Optional<String> getSslCertificate() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.http.ssl.certificate.file", String.class);
    }

    void onStart(@Observes StartupEvent ev) {
        // Ne pas enregistrer en développement (localhost)
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            LOG.info("Skipping HUB2 webhook registration in development mode");
            return;
        }

        // Vérifier si SSL est configuré
        Optional<String> sslCertificate = getSslCertificate();
        Optional<Integer> sslPort = getSslPort();

        // Calculer l'URL du webhook de manière sécurisée
        boolean useHttps = sslCertificate.isPresent() && !sslCertificate.get().isEmpty();
        String protocol = useHttps ? "https" : "http";
        int webhookPort = useHttps && sslPort.isPresent() ? sslPort.get() : port;
        String webhookUrl = protocol + "://" + host + ":" + webhookPort + "/webhooks/hub2/payment";

        registerWebhook(webhookUrl);
    }

    private void registerWebhook(String webhookUrl) {
        LOG.infof("Enregistrement du webhook HUB2: %s", webhookUrl);

        Map<String, Object> payload = new HashMap<>();
        payload.put("url", webhookUrl);
        payload.put("events", List.of(
                "payment.created",
                "payment.pending",
                "payment.succeeded",
                "payment.action_required",
                "payment.failed",
                "payment_intent.created",
                "payment_intent.processing",
                "payment_intent.succeeded",
                "payment_intent.action_required",
                "payment_intent.payment_failed"
        ));
        payload.put("description", "Webhook pour les notifications de paiement Flot");
        payload.put("metadata", Map.of("application", "flot-backend"));

        hub2Client.registerWebhook(
                hub2Config.getApiKey(),
                hub2Config.getMerchantId(),
                hub2Config.getEnvironment(),
                payload
        ).subscribe().with(
                result -> {
                    LOG.infof("Webhook HUB2 enregistré avec succès. ID: %s", result.get("id"));
                    // Important: stockez le secret du webhook de manière sécurisée
                    if (result.containsKey("secret")) {
                        LOG.infof("Webhook secret: %s", result.get("secret"));
                        // Idéalement, stockez ce secret dans une base de données sécurisée
                    }
                },
                error -> LOG.errorf("Erreur lors de l'enregistrement du webhook: %s", error.getMessage())
        );
    }
}