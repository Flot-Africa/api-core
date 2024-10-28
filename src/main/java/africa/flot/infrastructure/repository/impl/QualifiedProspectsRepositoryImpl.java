package africa.flot.infrastructure.repository.impl;

import africa.flot.application.ports.QualifiedProspectsRepository;
import africa.flot.domain.model.QualifiedProspect;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class QualifiedProspectsRepositoryImpl implements QualifiedProspectsRepository {

    @Override
    @WithSession
    public Uni<Boolean> isQualified(UUID leadId) {
        return QualifiedProspect.<QualifiedProspect>find("leadId = ?1 and status = true", leadId)
                .firstResult()
                .map(Objects::nonNull);
    }
}