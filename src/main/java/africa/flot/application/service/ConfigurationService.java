package africa.flot.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 🔧 SERVICE DE CONFIGURATION
 */
@ApplicationScoped
public class ConfigurationService {

    private static final Logger LOG = Logger.getLogger(ConfigurationService.class);

    /**
     * Configuration des seuils d'évaluation
     */
    public Map<String, BigDecimal> getSeuilsEvaluation() {
        return Map.of(
                "SEUIL_REUSSITE", BigDecimal.valueOf(16.0),
                "SEUIL_EXCELLENCE", BigDecimal.valueOf(18.0),
                "SEUIL_FORMATION", BigDecimal.valueOf(14.0),
                "SEUIL_ANOMALIE", BigDecimal.valueOf(3.0)
        );
    }

    /**
     * Configuration de l'intégration Laravel
     */
    public Map<String, String> getConfigLaravel() {
        return Map.of(
                "LARAVEL_API_URL", getEnvProperty("LARAVEL_API_URL", "http://localhost:8000/api"),
                "LARAVEL_WEBHOOK_SECRET", getEnvProperty("LARAVEL_WEBHOOK_SECRET", ""),
                "SYNC_ENABLED", getEnvProperty("SYNC_ENABLED", "true")
        );
    }

    /**
     * Configuration des notifications
     */
    public Map<String, String> getConfigNotifications() {
        return Map.of(
                "EMAIL_ENABLED", getEnvProperty("EMAIL_ENABLED", "true"),
                "SMS_ENABLED", getEnvProperty("SMS_ENABLED", "false"),
                "WEBHOOK_ENABLED", getEnvProperty("WEBHOOK_ENABLED", "true")
        );
    }

    private String getEnvProperty(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }
}
