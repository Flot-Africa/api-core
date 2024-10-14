package africa.flot.domain.model;

import africa.flot.domain.model.enums.Habitation;
import africa.flot.domain.model.enums.SituationMatrimoniale;
import africa.flot.domain.model.enums.SubscriberStatus;
import africa.flot.domain.model.valueobject.Address;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "subscriber")
public class Subscriber extends PanacheEntityBase {

    @Id
    public UUID id;

    // Informations de base
    @Column(nullable = false)
    public String nom;

    @Column(nullable = false)
    public String prenom;

    @Column(nullable = true, unique = true)
    public String email;

    @Column(nullable = false, unique = true)
    public String telephone;

    @Column(name = "date_naissance")
    public LocalDate dateNaissance;


    @Column(name = "adresse")
    public Address address;

    // Informations professionnelles



    @Column(name = "date_obtention_permis")
    public LocalDate dateObtentionPermis;

    @Column(name = "date_debut_exercice_vtc")
    public LocalDate dateDebutExerciceVTC;

    @Column(name = "revenu_mensuel")
    public BigDecimal revenuMensuel;

    @Column(name = "charges_mensuelles")
    public BigDecimal chargesMensuelles;

    @Column(name = "situation_matrimoniale")
    @Enumerated(EnumType.STRING)
    public SituationMatrimoniale situationMatrimoniale;

    @Column(name = "habitation")
    @Enumerated(EnumType.STRING)
    public Habitation habitation;

    @Column(name = "revenuesConjoint")
    public BigDecimal revenuesConjoint;

    @Column(name = "nombre_enfants")
    public Integer nombreEnfants;


    // Informations légales
    @Column(name = "numero_cni", unique = true)
    public String numeroCNI;

    @Column(name = "numero_siret", unique = true)
    public String numeroSIRET;

    @Column(name = "numero_rcs")
    public String numeroRCS;

    @Column(name = "numero_tva")
    public String numeroTVA;


    @Column
    public String nationalite;

    // Statut et progression
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SubscriberStatus status = SubscriberStatus.LEADS;

  /*  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public KYCDocuments kycDocuments;

    @OneToOne(cascade = CascadeType.ALL)
    public Evaluation evaluation;

    @OneToOne(mappedBy = "subscriber")
    public Account account;*/

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }


    public LocalDate getDateObtentionPermis() {
        return dateObtentionPermis;
    }

    public void setDateObtentionPermis(LocalDate dateObtentionPermis) {
        this.dateObtentionPermis = dateObtentionPermis;
    }

    public LocalDate getDateDebutExerciceVTC() {
        return dateDebutExerciceVTC;
    }

    public void setDateDebutExerciceVTC(LocalDate dateDebutExerciceVTC) {
        this.dateDebutExerciceVTC = dateDebutExerciceVTC;
    }

    public BigDecimal getRevenuMensuel() {
        return revenuMensuel;
    }

    public void setRevenuMensuel(BigDecimal revenuMensuel) {
        this.revenuMensuel = revenuMensuel;
    }

    public BigDecimal getChargesMensuelles() {
        return chargesMensuelles;
    }

    public void setChargesMensuelles(BigDecimal chargesMensuelles) {
        this.chargesMensuelles = chargesMensuelles;
    }

    public SituationMatrimoniale getSituationMatrimoniale() {
        return situationMatrimoniale;
    }

    public void setSituationMatrimoniale(SituationMatrimoniale situationMatrimoniale) {
        this.situationMatrimoniale = situationMatrimoniale;
    }

    public Habitation getHabitation() {
        return habitation;
    }

    public void setHabitation(Habitation habitation) {
        this.habitation = habitation;
    }

    public BigDecimal getRevenuesConjoint() {
        return revenuesConjoint;
    }

    public void setRevenuesConjoint(BigDecimal revenuesConjoint) {
        this.revenuesConjoint = revenuesConjoint;
    }

    public Integer getNombreEnfants() {
        return nombreEnfants;
    }

    public void setNombreEnfants(Integer nombreEnfants) {
        this.nombreEnfants = nombreEnfants;
    }

    public String getNumeroCNI() {
        return numeroCNI;
    }

    public void setNumeroCNI(String numeroCNI) {
        this.numeroCNI = numeroCNI;
    }

    public String getNumeroSIRET() {
        return numeroSIRET;
    }

    public void setNumeroSIRET(String numeroSIRET) {
        this.numeroSIRET = numeroSIRET;
    }

    public String getNumeroRCS() {
        return numeroRCS;
    }

    public void setNumeroRCS(String numeroRCS) {
        this.numeroRCS = numeroRCS;
    }

    public String getNumeroTVA() {
        return numeroTVA;
    }

    public void setNumeroTVA(String numeroTVA) {
        this.numeroTVA = numeroTVA;
    }

    public String getNationalite() {
        return nationalite;
    }

    public void setNationalite(String nationalite) {
        this.nationalite = nationalite;
    }


    public SubscriberStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriberStatus status) {
        this.status = status;
    }


    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    // Méthodes utilitaires
    public void updateStatus(SubscriberStatus newStatus) {
        this.status = newStatus;
    }


}


