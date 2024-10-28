package africa.flot.infrastructure.repository.impl;

import africa.flot.application.ports.ScoringRepository;
import africa.flot.domain.model.LeadScore;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ScoringRepositoryImpl implements ScoringRepository, PanacheRepositoryBase<LeadScore, UUID> {

    @WithSession
    @Override
    public Uni<Boolean> isScored(UUID leadId) {
        return LeadScore.<LeadScore>find("leadId", leadId)
                .firstResult()
                .map(Objects::nonNull); // retourne true si un score existe
    }

    @WithSession
    @Override
    public Uni<Void> save(LeadScore leadScore) {
        return persistAndFlush(leadScore).replaceWithVoid();
    }
}
