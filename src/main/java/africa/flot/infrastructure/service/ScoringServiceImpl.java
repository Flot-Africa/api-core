package africa.flot.infrastructure.service;

import africa.flot.application.ports.CanScoringRepository;
import africa.flot.application.ports.QualifiedProspectsRepository;
import africa.flot.application.ports.ScoringRepository;
import africa.flot.application.ports.ScoringService;
import africa.flot.infrastructure.repository.KYBRepository;

import africa.flot.domain.model.Lead;
import africa.flot.domain.model.LeadScore;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@ApplicationScoped
public class ScoringServiceImpl implements ScoringService {
    private static final Logger LOG = Logger.getLogger(ScoringServiceImpl.class);

    @Inject
    KYBRepository kybDocumentsRepository;

    @Inject
    QualifiedProspectsRepository qualifiedProspectsRepository;

    @Inject
    ScoringRepository scoringRepository;

    @Inject
    CanScoringRepository canScoringRepository;

    // Constants de score maximum pour chaque catégorie
    private static final int MAX_SCORE = 935;
    private static final int PERSONAL_DATA_MAX = 250;
    private static final int VTC_EXPERIENCE_MAX = 240;
    private static final int DRIVING_RECORD_MAX = 170;

    @Override
    public Uni<DetailedScore> calculateScore(Lead lead) {
        LOG.infof("Début du calcul du score pour le lead %s", lead.getId());

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

                                DetailedScore score = calculateDetailedScore(lead);
                                return persistScore(lead.getId(), score)
                                        .onItem().transform(v -> score);
                            });
                })
                .onFailure().invoke(e ->
                        LOG.errorf(e, "Erreur lors du calcul du score pour le lead %s", lead.getId())
                );
    }

    private DetailedScore calculateDetailedScore(Lead lead) {
        LOG.infof("Début du calcul du score détaillé pour le lead %s", lead.getId());

        try {
            double personalDataScore = calculatePersonalDataScore(lead);
            double vtcExperienceScore = calculateVtcExperienceScore(lead);
            double drivingRecordScore = calculateDrivingRecordScore(lead);

            // Conversion des scores en points réels
            int finalPersonalDataScore = (int) (personalDataScore * PERSONAL_DATA_MAX / 10);
            int finalVtcExperienceScore = (int) (vtcExperienceScore * VTC_EXPERIENCE_MAX / 10);
            int finalDrivingRecordScore = (int) (drivingRecordScore * DRIVING_RECORD_MAX / 10);

            int rawTotalScore = finalPersonalDataScore + finalVtcExperienceScore + finalDrivingRecordScore;
            int finalTotalScore = Math.min(rawTotalScore, MAX_SCORE);

            LOG.infof("Scores détaillés pour le lead %s:\n" +
                            "- Score données personnelles: %.1f/10 (%d/%d)\n" +
                            "- Score expérience VTC: %.1f/10 (%d/%d)\n" +
                            "- Score dossier de conduite: %.1f/10 (%d/%d)\n" +
                            "- Score total final: %.1f/10 (%d/%d)",
                    lead.getId(),
                    personalDataScore, finalPersonalDataScore, PERSONAL_DATA_MAX,
                    vtcExperienceScore, finalVtcExperienceScore, VTC_EXPERIENCE_MAX,
                    drivingRecordScore, finalDrivingRecordScore, DRIVING_RECORD_MAX,
                    (finalTotalScore * 10.0 / MAX_SCORE), finalTotalScore, MAX_SCORE);

            return new DetailedScore(finalPersonalDataScore, finalVtcExperienceScore,
                    finalDrivingRecordScore, finalTotalScore);

        } catch (Exception e) {
            LOG.errorf(e, "Erreur lors du calcul du score détaillé pour le lead %s", lead.getId());
            throw e;
        }
    }

    private double calculatePersonalDataScore(Lead lead) {
        LOG.infof("Calcul du score des données personnelles pour le lead %s", lead.getId());
        double score = 0;

        // Age (4 points)
        int age = lead.getBirthDate() != null ? Period.between(lead.getBirthDate(), LocalDate.now()).getYears() : -1;
        double ageScore = (age >= 24 && age <= 54) ? 4 : (age > 54) ? 2.6 : (age != -1) ? 1.2 : 0;
        score += ageScore;
        LOG.infof("Score age: %.1f/4", ageScore);

        // Statut marital (4 points)
        MaritalStatus status = lead.getMaritalStatus();
        double maritalScore = (status == MaritalStatus.MARIE) ? 4 : (status != null) ? 2 : 0;
        score += maritalScore;
        LOG.infof("Score statut marital: %.1f/4", maritalScore);

        // Enfants (2 points)
        Integer childrenCount = lead.getChildrenCount();
        double childrenScore = (childrenCount != null) ? (childrenCount >= 2 ? 2 : childrenCount == 1 ? 1.2 : 0) : 0;
        score += childrenScore;
        LOG.infof("Score enfants: %.1f/2", childrenScore);

        LOG.infof("Score final données personnelles: %.1f/10", score);
        return score;
    }

    private double calculateVtcExperienceScore(Lead lead) {
        LOG.infof("Calcul du score d'expérience VTC pour le lead %s", lead.getId());
        double score = 0;

        // Expérience temporelle (7.5 points)
        if (lead.getVtcStartDate() != null) {
            long experienceMonths = Period.between(lead.getVtcStartDate(), LocalDate.now()).toTotalMonths();
            double experienceScore = (experienceMonths >= 24) ? 7.5 : (experienceMonths >= 12) ? 5 : 2.5;
            score += experienceScore;
            LOG.infof("Score expérience temporelle: %.1f/7.5", experienceScore);
        }

        // Note plateforme (2.5 points)
        Double averageRating = lead.getAverageVtcRating();
        if (averageRating != null) {
            double ratingScore = (averageRating >= 4.5) ? 2.5 : (averageRating >= 4.0) ? 1.5 : 0.5;
            score += ratingScore;
            LOG.infof("Score note plateforme: %.1f/2.5", ratingScore);
        }

        LOG.infof("Score final expérience VTC: %.1f/10", score);
        return score;
    }

    private double calculateDrivingRecordScore(Lead lead) {
        LOG.infof("Calcul du score du dossier de conduite pour le lead %s", lead.getId());
        double score = 0;

        // Ancienneté permis (4.7 points)
        if (lead.getPermitAcquisitionDate() != null) {
            int licenseAgeYears = Period.between(lead.getPermitAcquisitionDate(), LocalDate.now()).getYears();
            double licenseScore = (licenseAgeYears >= 5) ? 4.7 : (licenseAgeYears >= 3) ? 3.5 : 1.8;
            score += licenseScore;
            LOG.infof("Score ancienneté permis: %.1f/4.7", licenseScore);
        }

        // Points permis (3.5 points)
        Integer pointsRemaining = lead.getLicensePoints();
        if (pointsRemaining != null) {
            double pointsScore = (pointsRemaining == 12) ? 3.5 : (pointsRemaining >= 9) ? 2.5 :
                    (pointsRemaining >= 6) ? 1.5 : 0.5;
            score += pointsScore;
            LOG.infof("Score points permis: %.1f/3.5", pointsScore);
        }

        // Infractions actuelles (pénalités jusqu'à -3.5 points)
        Integer currentInfractionsCount = lead.getCurrentInfractionsCount();
        BigDecimal totalFineAmount = lead.getTotalFineAmount();
        if (currentInfractionsCount != null && totalFineAmount != null && currentInfractionsCount > 0) {
            double penalty = totalFineAmount.compareTo(new BigDecimal("50000")) <= 0 ? 1 :
                    totalFineAmount.compareTo(new BigDecimal("100000")) <= 0 ? 2 : 3.5;
            score -= penalty;
            LOG.infof("Pénalité infractions actuelles: -%.1f", penalty);
        }

        // Historique infractions (pénalités jusqu'à -2.5 points)
        Integer historicalInfractionsCount = lead.getHistoricalInfractionsCount();
        if (historicalInfractionsCount != null) {
            double historyPenalty = (historicalInfractionsCount > 10) ? 2.5 :
                    (historicalInfractionsCount > 5) ? 1.5 :
                            (historicalInfractionsCount > 0) ? 0.5 : 0;
            score -= historyPenalty;
            LOG.infof("Pénalité historique infractions: -%.1f", historyPenalty);
        }

        // Expérience véhicule électrique (1.8 points)
        Boolean hasElectricVehicleExperience = lead.getHasElectricVehicleExperience();
        if (Boolean.TRUE.equals(hasElectricVehicleExperience)) {
            score += 1.8;
            LOG.infof("Bonus véhicule électrique: +1.8");
        }

        double finalScore = Math.max(0, Math.min(score, 10));
        LOG.infof("Score final dossier de conduite: %.1f/10", finalScore);
        return finalScore;
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

    private Uni<Void> persistScore(UUID leadId, DetailedScore score) {
        LOG.debugf("Sauvegarde du score pour le lead %s", leadId);

        LeadScore leadScore = new LeadScore();
        leadScore.setId(UUID.randomUUID());
        leadScore.setLeadId(leadId);
        leadScore.setPersonalDataScore(score.getPersonalDataScore());
        leadScore.setVtcExperienceScore(score.getVtcExperienceScore());
        leadScore.setDrivingRecordScore(score.getDrivingRecordScore());
        leadScore.setTotalScore(score.getTotalScore());
        leadScore.setCreatedAt(LocalDateTime.now());

        return scoringRepository.save(leadScore)
                .onItem().invoke(() -> LOG.infof("Score sauvegardé avec succès pour le lead %s", leadId));
    }

    private record ValidationResult(boolean valid, String message) {
    }
}

