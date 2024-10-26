package africa.flot.application.repository;


import africa.flot.domain.model.KYBDocuments;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class KYBRepository implements PanacheRepositoryBase<KYBDocuments, UUID> {

    @WithSession // Ajout de l'annotation pour assurer une session disponible
    public Uni<Optional<KYBDocuments>> findByLeadId(UUID leadId) {
        return find("leadId = ?1", leadId)
                .firstResult()
                .map(Optional::ofNullable);
    }
}