package africa.flot.infrastructure.repository;

import africa.flot.domain.model.DanayaVerificationResults;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DanayaVerificationRepository implements PanacheRepositoryBase<DanayaVerificationResults, UUID> {

    public Uni<Optional<DanayaVerificationResults>> findByLeadId(UUID leadId) {
        return find("leadId", leadId)
                .firstResult()
                .map(Optional::ofNullable);
    }
}