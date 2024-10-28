package africa.flot.domain.model.valueobject;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class Address {

    @NotBlank(message = "La ville de naissance est obligatoire")
    @Size(max = 255, message = "La ville de naissance ne doit pas dépasser 255 caractères")
    private String villeNaissance;

    @NotBlank(message = "La zone de résidence est obligatoire")
    @Size(max = 255, message = "La zone de résidence ne doit pas dépasser 255 caractères")
    private String zoneResidence;

    @NotBlank(message = "Le pays de naissance est obligatoire")
    @Size(max = 255, message = "Le pays de naissance ne doit pas dépasser 255 caractères")
    private String paysNaissance;

    // Getters et Setters

}
