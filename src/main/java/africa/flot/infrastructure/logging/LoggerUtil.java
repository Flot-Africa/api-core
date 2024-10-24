package africa.flot.infrastructure.logging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class LoggerUtil {
    private final Logger danayaLogger;
    private final Logger auditLogger;
    private final Logger securityLogger;
    private final Logger errorLogger;

    public LoggerUtil() {
        this.danayaLogger = Logger.getLogger("africa.flot.infrastructure.dayana");
        this.auditLogger = Logger.getLogger("africa.flot.audit");
        this.securityLogger = Logger.getLogger("africa.flot.security");
        this.errorLogger = Logger.getLogger("africa.flot.error");
    }

    // Méthodes pour Danaya
    public void danayaInfo(String message) {
        danayaLogger.info(message);
    }

    public void danayaDebug(String message) {
        danayaLogger.debug(message);
    }

    public void danayaError(String message, Throwable t) {
        danayaLogger.error(message, t);
    }

    // Méthodes pour l'audit
    public void auditInfo(String message) {
        auditLogger.info(message);
    }

    public void auditAction(String user, String action, String details) {
        auditLogger.infof("User: %s | Action: %s | Details: %s", user, action, details);
    }

    // Méthodes pour la sécurité
    public void securityInfo(String message) {
        securityLogger.info(message);
    }

    public void securityWarn(String message) {
        securityLogger.warn(message);
    }

    public void securityAlert(String message) {
        securityLogger.errorf("SECURITY ALERT: %s", message);
    }

    // Méthodes pour les erreurs
    public void error(String message) {
        errorLogger.error(message);
    }

    public void error(String message, Throwable t) {
        errorLogger.error(message, t);
    }

    public void errorWithContext(String context, String message, Throwable t) {
        errorLogger.errorf("[%s] %s: %s", context, message, t.getMessage(), t);
    }
}