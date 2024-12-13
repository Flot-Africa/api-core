package africa.flot.infrastructure.repository;

import africa.flot.domain.model.Lead;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class LeadRepository implements PanacheRepositoryBase<Lead, UUID> {

    // Méthode pour trouver un lead par son ID
    public Uni<Lead> findById(UUID leadId) {
        return find("id", leadId).firstResult();
    }

    @WithSession
    public Uni<Boolean> existsById(UUID leadId) {
        return find("id", leadId).firstResult()
                .map(Objects::nonNull);
    }

}