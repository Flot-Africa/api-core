package africa.flot.infrastructure.repository.impl;

import africa.flot.application.ports.CanScoringRepository;
import africa.flot.domain.model.Scoring;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class CanScoringRepositoryImpl implements CanScoringRepository, PanacheRepositoryBase<Scoring, UUID> {

    @WithSession
    @Override
    public Uni<Boolean> canScoring(UUID leadId) {
        return Scoring.<Scoring>find("leadId = ?1 and status = true", leadId)
                .firstResult()
                .map(Objects::nonNull);
    }
}
