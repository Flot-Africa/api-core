package africa.flot.application.repository;

import africa.flot.domain.model.Lead;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
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

    // Méthode pour vérifier si un lead existe
    public Uni<Boolean> existsById(UUID leadId) {
        return find("id", leadId).firstResult()
                .map(Objects::nonNull);
    }
}