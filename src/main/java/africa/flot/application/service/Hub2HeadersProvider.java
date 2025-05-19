package africa.flot.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.HashMap;

@ApplicationScoped
public class Hub2HeadersProvider {
    @ConfigProperty(name = "quarkus.hub2.merchant-id")
    String merchantId;

    @ConfigProperty(name = "quarkus.hub2.environment")
    String environment;

    public Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("merchantId", merchantId);
        headers.put("environment", environment);
        headers.put("ApiKey", System.getenv("HUB2_API_KEY")); // Ajout de l'ApiKey si nécessaire
        return headers;
    }
}