package africa.flot.domain.model;

import africa.flot.domain.model.enums.SituationMatrimoniale;
import africa.flot.domain.model.enums.SubscriberStatus;
import africa.flot.domain.model.valueobject.Address;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(name = "subscriber")
public class Subscriber extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    // Informations de base
    @Column(nullable = false)
    public String nom;

    @Column(nullable = false)
    public String prenom;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false, unique = true)
    public String telephone;

    @Column(name = "date_naissance")
    public LocalDate dateNaissance;

    @Embedded
    public Address adresse;

    // Informations professionnelles
    @Column(name = "date_obtention_permis")
    public LocalDate dateObtentionPermis;

    @Column(name = "date_debut_exercice_vtc")
    public LocalDate dateDebutExerciceVTC;

    @Column(name = "revenu_mensuel")
    public Double revenuMensuel;

    @Column(name = "charges_mensuelles")
    public Double chargesMensuelles;

    @Column(name = "situation_matrimoniale")
    @Enumerated(EnumType.STRING)
    public SituationMatrimoniale situationMatrimoniale;

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

    @Column(name = "pays_naissance")
    public String paysNaissance;

    @Column(name = "ville_naissance")
    public String villeNaissance;

    // Statut et progression
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SubscriberStatus status = SubscriberStatus.LEADS;

    @OneToOne(cascade = CascadeType.ALL)
    public KYCDocuments kycDocuments;

    @OneToOne(cascade = CascadeType.ALL)
    public Evaluation evaluation;

    @OneToOne(mappedBy = "subscriber")
    public Account account;

    // Méthodes utilitaires
    public void updateStatus(SubscriberStatus newStatus) {
        this.status = newStatus;
    }
}


