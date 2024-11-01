package africa.flot.infrastructure.service.dayana;

import africa.flot.application.dto.query.DanayaVerificationResult;
import africa.flot.application.exceptions.DocumentNotReadyException;
import africa.flot.infrastructure.logging.LoggerUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.UUID;

@ApplicationScoped
public class VerificationPoller {

    @Inject
    LoggerUtil logger;

    @Inject
    DanayaApiClient danayaApiClient;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.polling-interval-seconds", defaultValue = "5")
    int pollingIntervalSeconds;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.initial-delay-seconds", defaultValue = "3")
    int initialDelaySeconds;

    @ConfigProperty(name = "quarkus.rest-client.danaya-api.max-polling-attempts", defaultValue = "60")
    int maxPollingAttempts;

    public Uni<DanayaVerificationResult> pollVerificationStatus(UUID verificationUuid) {
        return Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofSeconds(initialDelaySeconds))
                .flatMap(ignored -> pollVerificationStatus(verificationUuid, 0));
    }

    private Uni<DanayaVerificationResult> pollVerificationStatus(UUID verificationUuid, int attemptCount) {
        if (attemptCount >= maxPollingAttempts) {
            String errorMsg = String.format("Délai d'attente dépassé [uuid=%s, tentatives=%d]", verificationUuid, attemptCount);
            logger.error(errorMsg);
            logger.auditAction("SYSTEM", "VERIFICATION_TIMEOUT", String.format("Timeout vérification [uuid=%s, attempts=%d]", verificationUuid, attemptCount));
            return Uni.createFrom().failure(new RuntimeException(errorMsg));
        }

        return danayaApiClient.getVerificationStatus(verificationUuid)
                .onFailure().transform(this::handleVerificationStatusFailure)
                .onFailure(DocumentNotReadyException.class).recoverWithUni(() -> retryPollingStatus(verificationUuid, attemptCount))
                .flatMap(this::handleVerificationStatusResult);
    }

    private Throwable handleVerificationStatusFailure(Throwable error) {
        if (error.getMessage().contains("404")) {
            logger.danayaDebug("Document en cours d'initialisation, nouvelle tentative après délai.");
            return new DocumentNotReadyException();
        }
        return error;
    }

    private Uni<DanayaVerificationResult> retryPollingStatus(UUID verificationUuid, int attemptCount) {
        return Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofSeconds(pollingIntervalSeconds))
                .flatMap(ignored -> pollVerificationStatus(verificationUuid, attemptCount + 1));
    }

    private Uni<DanayaVerificationResult> handleVerificationStatusResult(DanayaVerificationResult result) {
        String status = result.getStatus();
        logger.danayaDebug(String.format("Statut vérification [uuid=%s, status=%s]", result.getId(), status));
        switch (status) {
            case "EN_COURS" -> {
                return retryPollingStatus(result.getId(), 0);
            }
            case "A_TRAITER" -> {
                logger.auditAction("SYSTEM", "VERIFICATION_SUCCESS", String.format("Vérification réussie [uuid=%s]", result.getId()));
                return Uni.createFrom().item(result);
            }
            case "ERREUR" -> {
                return Uni.createFrom().failure(new RuntimeException(String.format("Échec de la vérification [uuid=%s]", result.getId())));
            }
            case null, default -> {
                return Uni.createFrom().failure(new RuntimeException(String.format("Statut de vérification invalide [uuid=%s, status=%s]", result.getId(), status)));
            }
        }
    }
}
