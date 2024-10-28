package africa.flot.application.ports;

import africa.flot.domain.model.Lead;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.smallrye.mutiny.Uni;

public interface ScoringService {
    Uni<DetailedScore> calculateScore(Lead lead);
}
