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

    private DetailedScore calculateDetailedScore(Lead lead) {
        LOG.infof("Début du calcul du score détaillé pour le lead %s", lead.getId());

        try {
            int personalDataScore = calculatePersonalDataScore(lead);
            int vtcExperienceScore = calculateVtcExperienceScore(lead);
            int drivingRecordScore = calculateDrivingRecordScore(lead);

            int rawTotalScore = personalDataScore + vtcExperienceScore + drivingRecordScore;
            int finalTotalScore = Math.min(rawTotalScore, MAX_SCORE);

            LOG.infof("Scores détaillés pour le lead %s:\n" +
                            "- Score données personnelles: %d/%d\n" +
                            "- Score expérience VTC: %d/%d\n" +
                            "- Score dossier de conduite: %d/%d\n" +
                            "- Score total (avant plafond): %d\n" +
                            "- Score total final: %d/%d",
                    lead.getId(), personalDataScore, PERSONAL_DATA_MAX,
                    vtcExperienceScore, VTC_EXPERIENCE_MAX,
                    drivingRecordScore, DRIVING_RECORD_MAX,
                    rawTotalScore, finalTotalScore, MAX_SCORE);

            return new DetailedScore(personalDataScore, vtcExperienceScore, drivingRecordScore, finalTotalScore);

        } catch (Exception e) {
            LOG.errorf(e, "Erreur lors du calcul du score détaillé pour le lead %s", lead.getId());
            throw e;
        }
    }

    private int calculatePersonalDataScore(Lead lead) {
        LOG.infof("Calcul du score des données personnelles pour le lead %s", lead.getId());
        int score = 0;

        int age = lead.getBirthDate() != null ? Period.between(lead.getBirthDate(), LocalDate.now()).getYears() : -1;

        score += (age >= 24 && age <= 54) ? 100 : (age > 54) ? 65 : (age != -1) ? 30 : 0;

        LOG.infof("Score basé sur l'age de %s pour le lead %s: %d", age, lead.getId(), score);

        MaritalStatus status = lead.getMaritalStatus();
        score += (status == MaritalStatus.MARIE) ? 100 : (status != null) ? 50 : 0;

        LOG.infof("Score basé sur le statut matrimonial  %s pour le lead %s: %d", status, lead.getId(), score);

        Integer childrenCount = lead.getChildrenCount();
        score += (childrenCount != null) ? (childrenCount >= 2 ? 50 : childrenCount == 1 ? 30 : 0) : 0;

        LOG.infof("Score basé sur le nombre d'enfants  %s pour le lead %s: %d", childrenCount, lead.getId(), score);

        return Math.min(score, PERSONAL_DATA_MAX);
    }

    private int calculateDrivingRecordScore(Lead lead) {
        LOG.infof("Calcul du score du dossier de conduite pour le lead %s", lead.getId());
        int score = 0;

        if (lead.getPermitAcquisitionDate() != null) {
            int licenseAgeYears = Period.between(lead.getPermitAcquisitionDate(), LocalDate.now()).getYears();
            score += (licenseAgeYears >= 5) ? 80 : (licenseAgeYears >= 3) ? 60 : 30; // Réduit de 100 à 80
        }

        Integer pointsRemaining = lead.getLicensePoints();
        if (pointsRemaining != null) {
            score += (pointsRemaining == 12) ? 60 : (pointsRemaining >= 9) ? 40 : (pointsRemaining >= 6) ? 20 : 10; // Réduit de 70 à 60
        }

        Integer currentInfractionsCount = lead.getCurrentInfractionsCount();
        BigDecimal totalFineAmount = lead.getTotalFineAmount();
        if (currentInfractionsCount != null && totalFineAmount != null) {
            score -= (currentInfractionsCount > 0) ? (totalFineAmount.compareTo(new BigDecimal("50000")) <= 0 ? 20
                    : (totalFineAmount.compareTo(new BigDecimal("100000")) <= 0 ? 40 : 70)) : 0;
        }

        // Ajout de l'historique des infractions
        Integer historicalInfractionsCount = lead.getHistoricalInfractionsCount();
        if (historicalInfractionsCount != null) {
            score -= (historicalInfractionsCount > 10) ? 50 : (historicalInfractionsCount > 5) ? 30 : (historicalInfractionsCount > 0) ? 10 : 0;
        }

        // Ajouter le score basé sur l'expérience avec des véhicules électriques
        Boolean hasElectricVehicleExperience = lead.getHasElectricVehicleExperience();
        if (Boolean.TRUE.equals(hasElectricVehicleExperience)) {
            score += 30;
            LOG.infof("Bonus ajouté pour l'expérience avec des véhicules électriques pour le lead %s: 40 points", lead.getId());
        }

        LOG.infof("Score final du dossier de conduite pour le lead %s: %d", lead.getId(), score);

        return Math.min(Math.max(score, 0), DRIVING_RECORD_MAX);
    }

    private int calculateVtcExperienceScore(Lead lead) {
        LOG.infof("Calcul du score d'expérience VTC pour le lead %s", lead.getId());
        int score = 0;

        if (lead.getVtcStartDate() != null) {
            long experienceMonths = Period.between(lead.getVtcStartDate(), LocalDate.now()).toTotalMonths();
            score += (experienceMonths >= 24) ? 180 : (experienceMonths >= 12) ? 120 : 60; // Augmenté pour atteindre 240
        }

        // Ajout du score basé sur la note sur les plateformes VTC
        Double averageRating = lead.getAverageVtcRating();
        if (averageRating != null) {
            score += (averageRating >= 4.5) ? 60 : (averageRating >= 4.0) ? 40 : 20; // Augmenté proportionnellement
        }

        LOG.infof("Score basé sur la note des plateformes VTC pour le lead %s: %d", lead.getId(), score);
        return Math.min(score, VTC_EXPERIENCE_MAX);
    }



    // Classe interne pour encapsuler les résultats de validation
        private record ValidationResult(boolean valid, String message) {
    }
}

