package africa.flot.infrastructure.repository;


import africa.flot.domain.model.KYBDocuments;
import africa.flot.infrastructure.repository.impl.ScoringServiceImpl;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class KYBRepository implements PanacheRepositoryBase<KYBDocuments, UUID> {
    private static final Logger LOG = Logger.getLogger(ScoringServiceImpl.class);

    @WithSession // Ajout de l'annotation pour assurer une session disponible
    public Uni<Optional<KYBDocuments>> findByLeadId(UUID leadId) {
        return find("leadId = ?1", leadId)
                .firstResult()
                .map(Optional::ofNullable);
    }

    @WithSession
    public Uni<Boolean> areDocumentsValid(UUID leadId) {
        return find("leadId = ?1 and cniUploadee = true and permisConduiteUploade = true and justificatifDomicileUploade = true", leadId)
                .firstResult()
                .onItem().invoke(doc -> {
                    if (doc == null) {
                        LOG.warnf("Aucun document valide trouvé pour le lead %s", leadId);
                    } else {
                        LOG.infof("Documents valides trouvés pour le lead %s", leadId);
                    }
                })
                .map(Objects::nonNull);
    }

}