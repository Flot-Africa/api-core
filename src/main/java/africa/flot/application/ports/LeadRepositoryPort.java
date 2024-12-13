package africa.flot.application.ports;

import africa.flot.domain.model.Lead;
import io.smallrye.mutiny.Uni;

public interface LeadRepositoryPort {

    boolean findByEmail(String emailAddress);

    Uni<Lead> enregistrer(Lead generate);
}
