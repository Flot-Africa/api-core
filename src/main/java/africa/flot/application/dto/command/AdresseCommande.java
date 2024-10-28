package africa.flot.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdresseCommande {

    @NotBlank(message = "La ville de naissance est obligatoire")
    @Size(max = 255, message = "La ville de naissance ne doit pas dépasser 255 caractères")
    private String villeNaissance;

    @NotBlank(message = "La zone de residence est obligatoire")
    @Size(max = 255, message = "La zone de residence  ne doit pas dépasser 255 caractères")
    private String zoneResidence;


    @NotBlank(message = "Le pays de naissance est obligatoire")
    @Size(max = 255, message = "Le pays de naissance ne doit pas dépasser 255 caractères")
    public String paysNaissance;


    public AdresseCommande() {
    }

    public String getZoneResidence() {
        return zoneResidence;
    }

    public void setZoneResidence(String zoneResidence) {
        this.zoneResidence = zoneResidence;
    }

    public String getPaysNaissance() {
        return paysNaissance;
    }

    public void setPaysNaissance(String paysNaissance) {
        this.paysNaissance = paysNaissance;
    }

    public String getVilleNaissance() {
        return villeNaissance;
    }

    public void setVilleNaissance(String villeNaissance) {
        this.villeNaissance = villeNaissance;
    }
}
