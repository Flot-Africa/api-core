package africa.flot.application.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Hub2Config {

    @ConfigProperty(name = "hub2.api-key")
    String apiKey;

    @ConfigProperty(name = "hub2.merchant-id")
    String merchantId;

    @ConfigProperty(name = "hub2.environment", defaultValue = "sandbox")
    String environment;

    @ConfigProperty(name = "hub2.webhook-secret")
    String webhookSecret;

    public String getApiKey() {
        return apiKey;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}