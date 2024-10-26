package africa.flot.application.command.subscriberCommande;

import africa.flot.application.command.AdresseCommande;
import africa.flot.domain.model.enums.Habitation;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.enums.SubscriberStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;


public class CreerSubscriberCommande {

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Le numéro de téléphone doit être valide")
    private String telephone;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 255, message = "Le prénom ne doit pas dépasser 255 caractères")
    private String prenom;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    //
    @NotNull(message = "L'adresse est obligatoire")
    private AdresseCommande adresseCommande;

    //

    @NotNull(message = "La date d'obtention du permis est obligatoire")
    @PastOrPresent(message = "La date d'obtention du permis doit être dans le passé ou le présent")
    private LocalDate dateObtentionPermis;

    @NotNull(message = "La date de début d'exercice VTC est obligatoire")
    @PastOrPresent(message = "La date de début d'exercice VTC doit être dans le passé ou le présent")
    private LocalDate dateDebutExerciceVTC;

    @NotNull(message = "Le revenu mensuel est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le revenu mensuel doit être supérieur à 0")
    private BigDecimal revenuMensuel;

    @NotNull(message = "Les charges mensuelles sont obligatoires")
    @DecimalMin(value = "0.0", inclusive = true, message = "Les charges mensuelles doivent être supérieures ou égales à 0")
    private BigDecimal chargesMensuelles;

    @NotNull(message = "La situation matrimoniale est obligatoire")
    private MaritalStatus maritalStatus;

    @NotNull(message = "Le type d'habitation est obligatoire")
    private Habitation habitation;

    @NotNull(message = "Le nombre d'enfants est obligatoire")
    @Min(value = 0, message = "Le nombre d'enfants doit être supérieur ou égal à 0")
    private Integer nombreEnfants;

    @DecimalMin(value = "0.0", inclusive = true, message = "Les revenus du conjoint doivent être supérieurs ou égaux à 0")
    private BigDecimal revenuesConjoint;

    @NotNull(message = "Le statut est obligatoire")
    private SubscriberStatus status;


    public @NotBlank(message = "Le numéro de téléphone est obligatoire") @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Le numéro de téléphone doit être valide") String getTelephone() {
        return telephone;
    }

    public void setTelephone(@NotBlank(message = "Le numéro de téléphone est obligatoire") @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Le numéro de téléphone doit être valide") String telephone) {
        this.telephone = telephone;
    }

    public @NotBlank(message = "Le nom est obligatoire") @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères") String getNom() {
        return nom;
    }

    public void setNom(@NotBlank(message = "Le nom est obligatoire") @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères") String nom) {
        this.nom = nom;
    }

    public @NotBlank(message = "Le prénom est obligatoire") @Size(max = 255, message = "Le prénom ne doit pas dépasser 255 caractères") String getPrenom() {
        return prenom;
    }

    public void setPrenom(@NotBlank(message = "Le prénom est obligatoire") @Size(max = 255, message = "Le prénom ne doit pas dépasser 255 caractères") String prenom) {
        this.prenom = prenom;
    }

    public @NotNull(message = "La date de naissance est obligatoire") @Past(message = "La date de naissance doit être dans le passé") LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(@NotNull(message = "La date de naissance est obligatoire") @Past(message = "La date de naissance doit être dans le passé") LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }


    public @NotNull(message = "La date d'obtention du permis est obligatoire") @PastOrPresent(message = "La date d'obtention du permis doit être dans le passé ou le présent") LocalDate getDateObtentionPermis() {
        return dateObtentionPermis;
    }

    public void setDateObtentionPermis(@NotNull(message = "La date d'obtention du permis est obligatoire") @PastOrPresent(message = "La date d'obtention du permis doit être dans le passé ou le présent") LocalDate dateObtentionPermis) {
        this.dateObtentionPermis = dateObtentionPermis;
    }

    public @NotNull(message = "La date de début d'exercice VTC est obligatoire") @PastOrPresent(message = "La date de début d'exercice VTC doit être dans le passé ou le présent") LocalDate getDateDebutExerciceVTC() {
        return dateDebutExerciceVTC;
    }

    public void setDateDebutExerciceVTC(@NotNull(message = "La date de début d'exercice VTC est obligatoire") @PastOrPresent(message = "La date de début d'exercice VTC doit être dans le passé ou le présent") LocalDate dateDebutExerciceVTC) {
        this.dateDebutExerciceVTC = dateDebutExerciceVTC;
    }

    public @NotNull(message = "Le revenu mensuel est obligatoire") @DecimalMin(value = "0.0", inclusive = false, message = "Le revenu mensuel doit être supérieur à 0") BigDecimal getRevenuMensuel() {
        return revenuMensuel;
    }

    public void setRevenuMensuel(@NotNull(message = "Le revenu mensuel est obligatoire") @DecimalMin(value = "0.0", inclusive = false, message = "Le revenu mensuel doit être supérieur à 0") BigDecimal revenuMensuel) {
        this.revenuMensuel = revenuMensuel;
    }

    public @NotNull(message = "Les charges mensuelles sont obligatoires") @DecimalMin(value = "0.0", inclusive = true, message = "Les charges mensuelles doivent être supérieures ou égales à 0") BigDecimal getChargesMensuelles() {
        return chargesMensuelles;
    }

    public void setChargesMensuelles(@NotNull(message = "Les charges mensuelles sont obligatoires") @DecimalMin(value = "0.0", inclusive = true, message = "Les charges mensuelles doivent être supérieures ou égales à 0") BigDecimal chargesMensuelles) {
        this.chargesMensuelles = chargesMensuelles;
    }

    public @NotNull(message = "La situation matrimoniale est obligatoire") MaritalStatus getSituationMatrimoniale() {
        return maritalStatus;
    }

    public void setSituationMatrimoniale(@NotNull(message = "La situation matrimoniale est obligatoire") MaritalStatus maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public @NotNull(message = "Le type d'habitation est obligatoire") Habitation getHabitation() {
        return habitation;
    }

    public void setHabitation(@NotNull(message = "Le type d'habitation est obligatoire") Habitation habitation) {
        this.habitation = habitation;
    }

    public @NotNull(message = "Le nombre d'enfants est obligatoire") @Min(value = 0, message = "Le nombre d'enfants doit être supérieur ou égal à 0") Integer getNombreEnfants() {
        return nombreEnfants;
    }

    public void setNombreEnfants(@NotNull(message = "Le nombre d'enfants est obligatoire") @Min(value = 0, message = "Le nombre d'enfants doit être supérieur ou égal à 0") Integer nombreEnfants) {
        this.nombreEnfants = nombreEnfants;
    }

    public BigDecimal getRevenuesConjoint() {
        return revenuesConjoint;
    }

    public void setRevenuesConjoint(BigDecimal revenuesConjoint) {
        this.revenuesConjoint = revenuesConjoint;
    }

    public SubscriberStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriberStatus status) {
        this.status = status;
    }

    public @NotNull(message = "L'adresse est obligatoire") AdresseCommande getAdresseCommande() {
        return adresseCommande;
    }

    public void setAdresseCommande(AdresseCommande adresseCommande) {
        this.adresseCommande = adresseCommande;
    }
}
