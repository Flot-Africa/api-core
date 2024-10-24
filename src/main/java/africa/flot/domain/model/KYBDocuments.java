package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class KYBDocuments extends PanacheEntityBase {
    @Id
    @GeneratedValue
    public UUID id;

    public boolean cniUploadee;
    public int cniProgressionVerification;
    public boolean justificatifDomicileUploade;
    public boolean permisConduiteUploade;
    public int permisConduiteProgressionVerification;
    public boolean attestationCACFUploade;
    public boolean photoIdentiteUploade;
    public boolean releveBancaireUploade;
}
