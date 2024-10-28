package africa.flot.application.ports;

import africa.flot.domain.model.LeadScore;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

public interface ScoringRepository {
    Uni<Boolean> isScored(UUID leadId);
    Uni<Void> save(LeadScore leadScore);
}
