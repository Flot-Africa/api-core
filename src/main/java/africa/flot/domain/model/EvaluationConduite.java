package africa.flot.domain.model;
import africa.flot.domain.model.enums.StatutEvaluation;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author katinan.toure 24/05/2025 17:32
 * @project api-core
 */

@Entity
@Table(name = "evaluations_conduite",
        indexes = {
                @Index(name = "idx_chauffeur_date", columnList = "chauffeur_id, date_evaluation"),
                @Index(name = "idx_evaluateur_date", columnList = "evaluateur_id, date_evaluation"),
                @Index(name = "idx_statut", columnList = "statut"),
                @Index(name = "idx_score_total", columnList = "score_total")})
public class    EvaluationConduite extends PanacheEntityBase {

    @Id
    public Long id;

    @Column(name = "chauffeur_id")
    @NotNull
    public Long chauffeurId;

    @Column(name = "evaluateur_id")
    @NotNull
    public Long evaluateurId;

    @Column(name = "date_evaluation")
    @NotNull
    public LocalDate dateEvaluation;

    // Critères d'évaluation - 0.5 points maximum chacun
    @Column(name = "reglage_poste_conduite", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal reglagePosteConducte = BigDecimal.ZERO;

    @Column(name = "ceinture_securite", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal ceintureSecurite = BigDecimal.ZERO;

    @Column(name = "voyants_tableau_bord", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal voyantsTableauBord = BigDecimal.ZERO;

    @Column(name = "demarrage_douceur", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal demarrageDouceur = BigDecimal.ZERO;

    @Column(name = "frein_stationnement", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal freinStationnement = BigDecimal.ZERO;

    @Column(name = "utilisation_clignotants", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal utilisationClignotants = BigDecimal.ZERO;

    @Column(name = "commandes", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal commandes = BigDecimal.ZERO;

    @Column(name = "gestion_dos_anes", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal gestionDosAnes = BigDecimal.ZERO;

    @Column(name = "priorites_rond_points", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal prioritesRondPoints = BigDecimal.ZERO;

    @Column(name = "tenue_route", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal tenueRoute = BigDecimal.ZERO;

    @Column(name = "retroviseurs", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal retroviseurs = BigDecimal.ZERO;

    @Column(name = "anticipation_pietons_obstacles", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal anticipationPietonsObstacles = BigDecimal.ZERO;

    @Column(name = "virages_courbes", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal viragesCourbes = BigDecimal.ZERO;

    @Column(name = "passage_pietons", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal passagePietons = BigDecimal.ZERO;

    @Column(name = "calme_serenite", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal calmeSerenite = BigDecimal.ZERO;

    @Column(name = "accueil_client", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal accueilClient = BigDecimal.ZERO;

    @Column(name = "ambiance_cabine", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal ambianceCabine = BigDecimal.ZERO;

    @Column(name = "respect_2_roues", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal respect2Roues = BigDecimal.ZERO;

    @Column(name = "orientation_gps", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal orientationGps = BigDecimal.ZERO;

    @Column(name = "reaction_situations_tendues", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("0.5")
    public BigDecimal reactionSituationsTendues = BigDecimal.ZERO;

    // Critères d'évaluation - 1.0 point maximum chacun
    @Column(name = "position_mains", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal positionMains = BigDecimal.ZERO;

    @Column(name = "lecture_panneaux", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal lecturePanneaux = BigDecimal.ZERO;

    @Column(name = "respect_limitations_vitesse", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal respectLimitationsVitesse = BigDecimal.ZERO;

    @Column(name = "stops_feux", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal stopsFeux = BigDecimal.ZERO;

    @Column(name = "angles_morts", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal anglesMorts = BigDecimal.ZERO;

    @Column(name = "distances_securite", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal distancesSecurite = BigDecimal.ZERO;

    @Column(name = "freinage_progressif", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal freinageProgressif = BigDecimal.ZERO;

    @Column(name = "gestion_imprevus", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal gestionImprevus = BigDecimal.ZERO;

    @Column(name = "confort_global_conduite", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal confortGlobalConduite = BigDecimal.ZERO;

    @Column(name = "observation_generale_finale", precision = 2, scale = 1)
    @DecimalMin("0.0") @DecimalMax("1.0")
    public BigDecimal observationGeneraleFinale = BigDecimal.ZERO;

    // Score total et métadonnées
    @Column(name = "score_total", precision = 3, scale = 1)
    @DecimalMin("0.0") @DecimalMax("20.0")
    public BigDecimal scoreTotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    public String commentaires;

    @Column(columnDefinition = "TEXT")
    public String recommandations;

    @Enumerated(EnumType.STRING)
    public StatutEvaluation statut ;//= StatutEvaluation.EN_ATTENTE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;


    // Méthodes utilitaires

    /**
     * Calcule automatiquement le score total
     */
    public BigDecimal calculerScoreTotal() {
        BigDecimal total = BigDecimal.ZERO;

        // Critères à 0.5 points
        BigDecimal[] criteresDemiPoint = {
                reglagePosteConducte, ceintureSecurite, voyantsTableauBord, demarrageDouceur,
                freinStationnement, utilisationClignotants, commandes, gestionDosAnes,
                prioritesRondPoints, tenueRoute, retroviseurs, anticipationPietonsObstacles,
                viragesCourbes, passagePietons, calmeSerenite, accueilClient, ambianceCabine,
                respect2Roues, orientationGps, reactionSituationsTendues
        };

        // Critères à 1.0 point
        BigDecimal[] criteresUnPoint = {
                positionMains, lecturePanneaux, respectLimitationsVitesse, stopsFeux,
                anglesMorts, distancesSecurite, freinageProgressif, gestionImprevus,
                confortGlobalConduite, observationGeneraleFinale
        };

        for (BigDecimal critere : criteresDemiPoint) {
            if (critere != null) {
                total = total.add(critere);
            }
        }

        for (BigDecimal critere : criteresUnPoint) {
            if (critere != null) {
                total = total.add(critere);
            }
        }

        return total.setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Met à jour le score total
     */
    @PrePersist
    @PreUpdate
    public void updateScoreTotal() {
        this.scoreTotal = calculerScoreTotal();
    }

    /**
     * Détermine si l'évaluation est réussie (≥ 16/20)
     */
    public boolean estReussie() {
        return scoreTotal.compareTo(new BigDecimal("16.0")) >= 0;
    }

    /**
     * Retourne le pourcentage de réussite
     */
    public BigDecimal getPourcentage() {
        return scoreTotal.multiply(new BigDecimal("5")).setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    // Méthodes de requête Panache

    /**
     * Trouve les évaluations par chauffeur
     */
    public static List<EvaluationConduite> findByChauffeur(Long chauffeurId) {
        return (List<EvaluationConduite>) find("chauffeurId", chauffeurId).list();
    }

    /**
     * Trouve les évaluations par évaluateur
     */
    public static List<EvaluationConduite> findByEvaluateur(Long evaluateurId) {
        return (List<EvaluationConduite>) find("evaluateurId", evaluateurId).list();
    }

    /**
     * Trouve les évaluations par statut
     */
    public static List<EvaluationConduite> findByStatut(StatutEvaluation statut) {
        return (List<EvaluationConduite>) find("statut", statut).list();
    }

    /**
     * Trouve les évaluations réussies
     */
    public static List<EvaluationConduite> findReussies() {
        return (List<EvaluationConduite>) find("scoreTotal >= ?1", new BigDecimal("16.0")).list();
    }

    /**
     * Trouve les évaluations d'une période
     */
    public static List<EvaluationConduite> findByPeriode(LocalDate debut, LocalDate fin) {
        return (List<EvaluationConduite>) find("dateEvaluation BETWEEN ?1 AND ?2", debut, fin).list();
    }

    /**
     * Calcule la moyenne des scores d'un chauffeur
     */
    public static BigDecimal moyenneScoreChauffeur(Long chauffeurId) {
        return (BigDecimal) find("SELECT AVG(scoreTotal) FROM EvaluationConduite WHERE chauffeurId = ?1", chauffeurId)
                .project(BigDecimal.class)
                .firstResult();
    }
}

