package africa.flot.application.dto.command;

import java.util.UUID;

public class ModifieKYBDocumentCommande {

    private UUID id;
    public boolean cniUploadee;
    public int cniProgressionVerification;
    public boolean justificatifDomicileUploade;
    public boolean permisConduiteUploade;
    public int permisConduiteProgressionVerification;
    public boolean attestationCACFUploade;
    public boolean photoIdentiteUploade;
    public boolean releveBancaireUploade;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean isCniUploadee() {
        return cniUploadee;
    }

    public void setCniUploadee(boolean cniUploadee) {
        this.cniUploadee = cniUploadee;
    }

    public int getCniProgressionVerification() {
        return cniProgressionVerification;
    }

    public void setCniProgressionVerification(int cniProgressionVerification) {
        this.cniProgressionVerification = cniProgressionVerification;
    }

    public boolean isJustificatifDomicileUploade() {
        return justificatifDomicileUploade;
    }

    public void setJustificatifDomicileUploade(boolean justificatifDomicileUploade) {
        this.justificatifDomicileUploade = justificatifDomicileUploade;
    }

    public boolean isPermisConduiteUploade() {
        return permisConduiteUploade;
    }

    public void setPermisConduiteUploade(boolean permisConduiteUploade) {
        this.permisConduiteUploade = permisConduiteUploade;
    }

    public int getPermisConduiteProgressionVerification() {
        return permisConduiteProgressionVerification;
    }

    public void setPermisConduiteProgressionVerification(int permisConduiteProgressionVerification) {
        this.permisConduiteProgressionVerification = permisConduiteProgressionVerification;
    }

    public boolean isAttestationCACFUploade() {
        return attestationCACFUploade;
    }

    public void setAttestationCACFUploade(boolean attestationCACFUploade) {
        this.attestationCACFUploade = attestationCACFUploade;
    }

    public boolean isPhotoIdentiteUploade() {
        return photoIdentiteUploade;
    }

    public void setPhotoIdentiteUploade(boolean photoIdentiteUploade) {
        this.photoIdentiteUploade = photoIdentiteUploade;
    }

    public boolean isReleveBancaireUploade() {
        return releveBancaireUploade;
    }

    public void setReleveBancaireUploade(boolean releveBancaireUploade) {
        this.releveBancaireUploade = releveBancaireUploade;
    }
}
