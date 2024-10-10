package africa.flot.application.command;

import africa.flot.domain.model.enums.SituationMatrimoniale;
import africa.flot.domain.model.valueobject.Address;
import jakarta.validation.constraints.*;

import java.time.LocalDate;


public record CreateSubscriberCommand(
        @NotBlank(message = "Le nom est obligatoire.")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire.")
        String prenom,

        @Email(message = "L'email doit être valide.")
        @NotBlank(message = "L'email est obligatoire.")
        String email,

        @NotBlank(message = "Le téléphone est obligatoire.")
        String telephone,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password,

        @Past(message = "La date de naissance doit être dans le passé.")
        LocalDate dateNaissance,

        @NotNull(message = "L'adresse est obligatoire.")
        Address adresse,

        @PastOrPresent(message = "La date d'obtention du permis doit être dans le passé ou le présent.")
        LocalDate dateObtentionPermis,

        @PastOrPresent(message = "La date de début d'exercice VTC doit être dans le passé ou le présent.")
        LocalDate dateDebutExerciceVTC,

        @DecimalMin(value = "0.0", inclusive = false, message = "Le revenu mensuel doit être positif.")
        Double revenuMensuel,

        @DecimalMin(value = "0.0", inclusive = false, message = "Les charges mensuelles doivent être positives.")
        Double chargesMensuelles,

        @NotNull(message = "La situation matrimoniale est obligatoire.")
        SituationMatrimoniale situationMatrimoniale,

        @Min(value = 0, message = "Le nombre d'enfants ne peut pas être négatif.")
        Integer nombreEnfants,

        @NotBlank(message = "Le numéro de CNI est obligatoire.")
        String numeroCNI,

        String numeroSIRET,
        String numeroRCS,
        String numeroTVA,

        @NotBlank(message = "La nationalité est obligatoire.")
        String nationalite,

        @NotBlank(message = "Le pays de naissance est obligatoire.")
        String paysNaissance,

        @NotBlank(message = "La ville de naissance est obligatoire.")
        String villeNaissance
) {
    // Vous pouvez ajouter des méthodes de validation supplémentaires ici si nécessaire
    public CreateSubscriberCommand {
        // Validation basique déjà effectuée par les annotations
    }
}
