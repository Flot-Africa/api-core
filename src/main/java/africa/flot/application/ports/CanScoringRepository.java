package africa.flot.application.ports;

import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface CanScoringRepository {
    Uni<Boolean> canScoring(UUID leadId);
}
