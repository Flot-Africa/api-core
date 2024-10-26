package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "kybdocuments")
@Data
public class KYBDocuments extends PanacheEntityBase {
    @Id
    private UUID id;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "cniuploadee")
    private boolean cniUploadee;

    @Column(name = "cniprogressionverification")
    private int cniProgressionVerification;

    @Column(name = "permisconduiteuploade")
    private boolean permisConduiteUploade;

    @Column(name = "permisconduiteprogressionverification")
    private int permisConduiteProgressionVerification;

    @Column(name = "photoidentiteuploade")
    private boolean photoIdentiteUploade;

    @Column(name = "justificatifdomicileuploade")
    private boolean justificatifDomicileUploade;

    @Column(name = "attestationcacfuploade")
    private boolean attestationCACFUploade;

    @Column(name = "relevebancaireuploade")
    private boolean releveBancaireUploade;
}
