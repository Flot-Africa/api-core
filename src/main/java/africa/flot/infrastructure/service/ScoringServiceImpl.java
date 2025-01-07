package africa.flot.infrastructure.service;

import africa.flot.application.ports.*;
import africa.flot.domain.model.*;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.infrastructure.repository.KYBRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

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

    // Scores sur 10
    private static final double MAX_SCORE = 10.0;
    private static final double PERSONAL_DATA_MAX = 10.0;
    private static final double DRIVING_RECORD_MAX = 10.0;
    private static final double VTC_EXPERIENCE_MAX = 10.0;

    // Coefficients
    private static final double PERSONAL_DATA_COEFF = 0.5; // 5/10
    private static final double DRIVING_RECORD_COEFF = 0.2; // 2/10
    private static final double VTC_EXPERIENCE_COEFF = 0.3; // 3/10

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
        leadScore.setDrivingRecordScore(score.getDrivingRecordScore());
        leadScore.setVtcExperienceScore(score.getVtcExperienceScore());
        leadScore.setTotalScore(score.getTotalScore());
        leadScore.setCreatedAt(LocalDateTime.now());

        return scoringRepository.save(leadScore)
                .onItem().invoke(() -> LOG.infof("Score sauvegardé avec succès pour le lead %s", leadId));
    }

    private DetailedScore calculateDetailedScore(Lead lead) {
        LOG.infof("Début du calcul du score détaillé pour le lead %s", lead.getId());

        try {
            double personalDataScore = calculatePersonalDataScore(lead);
            double drivingRecordScore = calculateDrivingRecordScore(lead);
            double vtcExperienceScore = calculateVtcExperienceScore(lead);

            // Application des coefficients
            double weightedTotal = (personalDataScore * PERSONAL_DATA_COEFF) +
                    (drivingRecordScore * DRIVING_RECORD_COEFF) +
                    (vtcExperienceScore * VTC_EXPERIENCE_COEFF);

            double finalTotalScore = Math.min(weightedTotal, MAX_SCORE);

            LOG.infof("""
                            Scores détaillés pour le lead %s:
                            - Score données personnelles: %.2f/10 (coeff: %.1f)
                            - Score dossier de conduite: %.2f/10 (coeff: %.1f)
                            - Score expérience VTC: %.2f/10 (coeff: %.1f)
                            - Score total pondéré: %.2f/10""",
                    lead.getId(), personalDataScore, PERSONAL_DATA_COEFF * 10,
                    drivingRecordScore, DRIVING_RECORD_COEFF * 10,
                    vtcExperienceScore, VTC_EXPERIENCE_COEFF * 10,
                    finalTotalScore);

            return new DetailedScore(personalDataScore, drivingRecordScore, vtcExperienceScore, finalTotalScore);

        } catch (Exception e) {
            LOG.errorf(e, "Erreur lors du calcul du score détaillé pour le lead %s", lead.getId());
            throw e;
        }
    }

    private double calculatePersonalDataScore(Lead lead) {
        LOG.infof("Calcul du score des données personnelles pour le lead %s", lead.getId());
        double score = 0;

        // Âge (2.5 points)
        int age = lead.getBirthDate() != null ? Period.between(lead.getBirthDate(), LocalDate.now()).getYears() : -1;
        score += (age >= 25 && age <= 50) ? 2.5 : (age > 50) ? 1.67 : (age != -1) ? 0.83 : 0;

        // Résidence (2 points)
        String residence = lead.getAddress().getZoneResidence();
        score += "Abidjan".equalsIgnoreCase(residence) ? 2 : 1;

        // Nationalité (2 points)
        String nationality = lead.getAddress().getPaysNaissance();
        score += "CI".equalsIgnoreCase(nationality) ? 2 : "CEDEAO".equalsIgnoreCase(nationality) ? 1.33 : 0.67;

        // Statut matrimonial (2 points)
        MaritalStatus status = lead.getMaritalStatus();
        score += (status == MaritalStatus.MARIE) ? 2 : 1;

        // Nombre d'enfants (1.5 points)
        Integer childrenCount = lead.getChildrenCount();
        score += (childrenCount != null) ? (childrenCount >= 2 ? 1.5 : childrenCount == 1 ? 1 : 0.5) : 0.5;

        return Math.min(score, PERSONAL_DATA_MAX);
    }

    private double calculateDrivingRecordScore(Lead lead) {
        LOG.infof("Calcul du score du dossier de conduite pour le lead %s", lead.getId());
        double score = 0;

        // Ancienneté du permis (3 points)
        if (lead.getPermitAcquisitionDate() != null) {
            int licenseAgeYears = Period.between(lead.getPermitAcquisitionDate(), LocalDate.now()).getYears();
            score += (licenseAgeYears >= 5) ? 3 : (licenseAgeYears >= 3) ? 2 : 1;
        }

        // Points restants (3 points)
        Integer pointsRemaining = lead.getLicensePoints();
        score += (pointsRemaining != null) ? (pointsRemaining >= 10 ? 3 : pointsRemaining >= 6 ? 2 : 1) : 0;

        // Accidents récents (2 points)
        Integer recentAccidents = lead.getAccidentCount();
        score += (recentAccidents != null) ? (recentAccidents == 0 ? 2 : recentAccidents == 1 ? 1 : 0) : 2;

        // Infractions en cours (2 points)
        boolean hasPendingFines = lead.getCurrentInfractionsCount() > 0;
        score += hasPendingFines ? 0 : 2;

        return Math.min(score, DRIVING_RECORD_MAX);
    }

    private double calculateVtcExperienceScore(Lead lead) {
        LOG.infof("Calcul du score d'expérience VTC pour le lead %s", lead.getId());
        double score = 0;

        // Expérience (5 points)
        if (lead.getVtcStartDate() != null) {
            long experienceMonths = Period.between(lead.getVtcStartDate(), LocalDate.now()).toTotalMonths();
            score += (experienceMonths >= 24) ? 5 : (experienceMonths >= 12) ? 3.5 : (experienceMonths >= 6) ? 2 : 0;
        }

        // Note moyenne (5 points)
        Double averageRating = lead.getAverageVtcRating();
        score += (averageRating != null) ? (averageRating >= 4.8 ? 5 : averageRating >= 4.5 ? 3.5 : averageRating >= 4.0 ? 2 : 0) : 0;

        return Math.min(score, VTC_EXPERIENCE_MAX);
    }

    private record ValidationResult(boolean valid, String message) {
    }
}