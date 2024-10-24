package africa.flot.domain.model;

import africa.flot.domain.model.enums.Habitation;
import africa.flot.domain.model.enums.SituationMatrimoniale;
import africa.flot.domain.model.enums.SubscriberStatus;
import africa.flot.domain.model.valueobject.Address;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "subscriber")
@Setter
@Getter
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


}


