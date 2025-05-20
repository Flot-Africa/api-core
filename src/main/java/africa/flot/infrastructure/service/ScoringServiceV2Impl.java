package africa.flot.infrastructure.service;

import africa.flot.application.ports.ScoringService;
import africa.flot.domain.model.*;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Alternative;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

/**
 * Implémentation du service de scoring selon le nouvel algorithme V2
 * basée uniquement sur les données du CRM (modèle Lead)
 */
@ApplicationScoped
@Alternative
public class ScoringServiceV2Impl implements ScoringService {
    private static final Logger LOG = Logger.getLogger(ScoringServiceV2Impl.class);

    @Inject
    africa.flot.infrastructure.repository.KYBRepository kybDocumentsRepository;

    @Inject
    africa.flot.application.ports.QualifiedProspectsRepository qualifiedProspectsRepository;

    @Inject
    africa.flot.application.ports.ScoringRepository scoringRepository;

    @Inject
    africa.flot.application.ports.CanScoringRepository canScoringRepository;

    // Scores selon la nouvelle structure (sur 100)
    private static final double SINFO_MAX = 40.0;
    private static final double SYANGO_MAX = 40.0;
    private static final double STEST_MAX = 20.0;
    private static final double PASSING_SCORE = 70.0;
    private static final double MINIMUM_TEST_SCORE = 15.0;

    @Override
    public Uni<DetailedScore> calculateScore(Lead lead) {
        LOG.infof("Début du calcul du score V2 pour le lead %s", lead.getId());

        return scoringRepository.isScored(lead.getId())
                .onItem().transformToUni(scoreExists -> {
                    if (scoreExists) {
                        LOG.warnf("Un score existe déjà pour le lead %s", lead.getId());
                        return Uni.createFrom().failure(new IllegalStateException("Le score existe déjà pour ce lead."));
                    }

                    return validateLeadAndDocuments(lead.getId())
                            .onItem().transformToUni(validationResult -> {
                                if (!validationResult.valid()) {
                                    LOG.errorf("Le lead %s n'est pas qualifié : %s", lead.getId(), validationResult.message());
                                    return Uni.createFrom().failure(new IllegalStateException(validationResult.message()));
                                }

                                DetailedScore score = calculateDetailedScoreV2(lead);
                                return persistScore(lead.getId(), score)
                                        .onItem().transform(v -> score);
                            });
                })
                .onFailure().invoke(e ->
                        LOG.errorf(e, "Erreur lors du calcul du score pour le lead %s", lead.getId())
                );
    }

    private Uni<ValidationResult> validateLeadAndDocuments(UUID leadId) {
        return qualifiedProspectsRepository.isQualified(leadId)
                .flatMap(isQualified -> {
                    if (Boolean.FALSE.equals(isQualified)) {
                        return Uni.createFrom().item(new ValidationResult(false, "Le lead n'est pas qualifié."));
                    }
                    return canScoringRepository.canScoring(leadId);
                })
                .flatMap(canScore -> {
                    if (Boolean.FALSE.equals(canScore)) {
                        return Uni.createFrom().item(new ValidationResult(false, "Le lead ne peut pas être scoré."));
                    }
                    return kybDocumentsRepository.areDocumentsValid(leadId)
                            .map(areDocsValid -> {
                                LOG.infof("Requirements valides pour le lead %s : %s", leadId, areDocsValid);
                                if (Boolean.FALSE.equals(areDocsValid)) {
                                    return new ValidationResult(false, "Les documents requis ne sont pas valides.");
                                }
                                return new ValidationResult(true, "Validation réussie.");
                            });
                });
    }

    @WithTransaction
    protected Uni<Void> persistScore(UUID leadId, DetailedScore score) {
        LOG.debugf("Sauvegarde du score pour le lead %s", leadId);

        LeadScore leadScore = new LeadScore();
        leadScore.setId(UUID.randomUUID());
        leadScore.setLeadId(leadId);
        leadScore.setPersonalDataScore(score.getPersonalDataScore());
        leadScore.setDrivingRecordScore(score.getDrivingRecordScore());
        leadScore.setVtcExperienceScore(score.getVtcExperienceScore());
        leadScore.setTotalScore(score.getTotalScore());
        leadScore.setApproved(score.getTotalScore() >= PASSING_SCORE / 10 && score.getVtcExperienceScore() > MINIMUM_TEST_SCORE / 10);
        leadScore.setCreatedAt(LocalDateTime.now());
        leadScore.setAlgorithmVersion("V2");

        return scoringRepository.save(leadScore)
                .onItem().invoke(() -> LOG.infof("Score V2 sauvegardé avec succès pour le lead %s", leadId));
    }

    private DetailedScore calculateDetailedScoreV2(Lead lead) {
        LOG.infof("Calcul du score V2 détaillé pour le lead %s", lead.getId());

        try {
            // Calcul du score informations personnelles (Sinfos)
            double sInfos = calculateInfoScore(lead);

            // Calcul du score activité VTC/Yango (Syango) à partir des données du Lead
            double sYango = calculateVtcScore(lead);

            // Calcul du score test de conduite (Stest) à partir des données du Lead
            double sTest = calculateTestScore(lead);

            // Calcul du score total
            double totalScore = sInfos + sYango + sTest;

            // Déterminer si le score est suffisant pour approbation
            boolean isApproved = (totalScore >= PASSING_SCORE && sTest > MINIMUM_TEST_SCORE);

            LOG.infof("""
                    Scores V2 détaillés pour le lead %s:
                    - Score informations personnelles: %.2f/40
                    - Score activité VTC: %.2f/40
                    - Score test de conduite: %.2f/20
                    - Score total: %.2f/100
                    - Décision: %s""",
                    lead.getId(), sInfos, sYango, sTest, totalScore,
                    isApproved ? "ACCEPTÉ" : "REFUSÉ");

            // Normaliser les scores sur 10 pour la compatibilité avec l'existant
            return new DetailedScore(
                    sInfos / 4,       // Sur 10 au lieu de 40
                    sYango / 4,       // Sur 10 au lieu de 40
                    sTest / 2,        // Sur 10 au lieu de 20
                    totalScore / 10   // Sur 10 au lieu de 100
            );

        } catch (Exception e) {
            LOG.errorf(e, "Erreur lors du calcul du score V2 détaillé pour le lead %s", lead.getId());
            throw e;
        }
    }

    private double calculateInfoScore(Lead lead) {
        double score = 0;

        // Pidentité = 5 si l'identité est vérifiée, sinon 0
        score += lead.getActive() ? 5 : 0;

        // Présidence = 5 si quartier premium, sinon 2
        String residence = lead.getAddress() != null ? lead.getAddress().getZoneResidence() : null;
        score += isPremiumLocation(residence) ? 5 : 2;

        // Penfants = 3 si ≤ 3, sinon 0
        Integer childrenCount = lead.getChildrenCount();
        score += (childrenCount != null && childrenCount <= 3) ? 3 : 0;

        // Pstatut_marital = 3 si marié, 1 si célibataire
        MaritalStatus status = lead.getMaritalStatus();
        score += (status == MaritalStatus.MARIE) ? 3 : 1;

        // Pétudes = 5 si niveau Bac ou plus, sinon 2
        boolean hasBac = lead.getEducationLevel() != null &&
                (lead.getEducationLevel().equalsIgnoreCase("BAC") ||
                        lead.getEducationLevel().equalsIgnoreCase("LICENCE") ||
                        lead.getEducationLevel().equalsIgnoreCase("MASTER"));
        score += hasBac ? 5 : 2;

        // Prevenus_informels = 5 si ≥ 250000 FCFA, sinon -5
        // Utilisons le salaire comme approximation des revenus informels
        double informalIncome = lead.getSalary() != null ? lead.getSalary().doubleValue() : 0;
        score += (informalIncome >= 250000) ? 5 : -5;

        // Pâge = 5 si 30 ≤ âge ≤ 45, sinon 0
        int age = lead.getBirthDate() != null ? Period.between(lead.getBirthDate(), LocalDate.now()).getYears() : 0;
        score += (age >= 30 && age <= 45) ? 5 : 0;

        // Pcharges = 4 si charges < 70%, sinon -4
        double expenseRatio = (lead.getExpenses() != null && lead.getSalary() != null && lead.getSalary().doubleValue() > 0) ?
                lead.getExpenses().doubleValue() / lead.getSalary().doubleValue() * 100 : 100;
        score += (expenseRatio < 70) ? 4 : -4;

        // Pendettement = 5 si raisonnable, sinon -5
        boolean reasonableDebt = isReasonableDebtLevel(lead);
        score += reasonableDebt ? 5 : -5;

        // Plafonner le score entre 0 et SINFO_MAX
        return Math.min(Math.max(score, 0), SINFO_MAX);
    }

    private double calculateVtcScore(Lead lead) {
        double score = 0;

        // Pcourses = 5 si >300 courses, sinon 0
        // Approx: Utilisez les données disponibles dans le Lead pour approximer le nombre de courses
        Integer estimatedRides = lead.getVtcRidesCount();
        score += (estimatedRides != null && estimatedRides > 300) ? 5 : 0;

        // Pancienneté_permis = 5 si >5 ans, sinon 0
        int licenseYears = lead.getPermitAcquisitionDate() != null ?
                Period.between(lead.getPermitAcquisitionDate(), LocalDate.now()).getYears() : 0;
        score += (licenseYears > 5) ? 5 : 0;

        // Pancienneté_yango = 5 si ≥ 2 ans, sinon 0
        int vtcYears = lead.getVtcStartDate() != null ?
                Period.between(lead.getVtcStartDate(), LocalDate.now()).getYears() : 0;
        score += (vtcYears >= 2) ? 5 : 0;

        // Prevenus_yango = 5 si >260000 FCFA, sinon 0
        // Approx: Utiliser le champ approprié dans Lead, ou estimer à partir d'autres données
        double estimatedRevenue = lead.getVtcMonthlyIncome() != null ?
                lead.getVtcMonthlyIncome().doubleValue() : 0;
        score += (estimatedRevenue > 260000) ? 5 : 0;

        // Pnote_yango = 10 si ≥ 4.90, sinon -10
        Double rating = lead.getAverageVtcRating();
        score += (rating != null && rating >= 4.90) ? 10 : -10;

        // Paccidents = 10 si aucun accident, sinon -10
        Integer accidents = lead.getAccidentCount();
        score += (accidents != null && accidents == 0) ? 10 : -10;

        // Plafonner le score entre 0 et SYANGO_MAX
        return Math.min(Math.max(score, 0), SYANGO_MAX);
    }

    private double calculateTestScore(Lead lead) {
        // Dans notre cas, nous utiliserons une donnée existante dans Lead
        // comme approximation du score de test de conduite
        // Par exemple, on peut utiliser un champ comme drivingTestScore
        Double testScore = lead.getDrivingTestScore();

        // Stest = note_test si note_test > 15, sinon 0 (refus automatique)
        return (testScore != null && testScore > MINIMUM_TEST_SCORE) ?
                Math.min(testScore, STEST_MAX) : 0;
    }

    private boolean isPremiumLocation(String location) {
        // Liste des quartiers premium (à définir selon les besoins business)
        String[] premiumLocations = {"Cocody", "Plateau", "Riviera", "Zone 4", "Deux Plateaux"};
        if (location == null) return false;

        for (String premium : premiumLocations) {
            if (location.contains(premium)) return true;
        }
        return false;
    }

    private boolean isReasonableDebtLevel(Lead lead) {
        // Logique d'évaluation de l'endettement (à définir selon les besoins business)
        // Par exemple, si l'endettement existant représente moins de 30% des revenus
        double debt = lead.getDebtAmount() != null ? lead.getDebtAmount().doubleValue() : 0.0;

        // Utilisez doubleValue() pour convertir BigDecimal en Double
        double income = lead.getSalary() != null ? lead.getSalary().doubleValue() : 0.0;

        if (income == 0.0) return false;

        double debtToIncomeRatio = debt / income;
        return debtToIncomeRatio < 0.3;
    }

    private record ValidationResult(boolean valid, String message) {
    }
}