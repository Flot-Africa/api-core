package africa.flot.infrastructure.interfaces.facade.query.impl;

import africa.flot.application.ports.ScoringService;
import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.enums.SituationMatrimoniale;
import africa.flot.domain.model.valueobject.DetailedScore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@ApplicationScoped
public class ScoringServiceImpl implements ScoringService {

    private static final int MAX_SCORE = 935;
    private static final int PERSONAL_DATA_MAX = 297;
    private static final int EV_COST_MAX = 170;
    private static final int INCOME_MAX = 85;
    private static final int VTC_EXPERIENCE_MAX = 297;
    private static final int DRIVING_RECORD_MAX = 85;

    @Override
    public Uni<DetailedScore> calculateScore(Subscriber subscriber) {
        return Uni.createFrom().item(() -> {
            int personalDataScore = calculatePersonalDataScore(subscriber);
            int evCostScore = calculateEvCostScore(subscriber);
            int incomeScore = calculateIncomeScore(subscriber);
            int vtcExperienceScore = calculateVtcExperienceScore(subscriber);
            int drivingRecordScore = calculateDrivingRecordScore(subscriber);

            int totalScore = personalDataScore + evCostScore + incomeScore +
                    vtcExperienceScore + drivingRecordScore;

            return new DetailedScore(
                    personalDataScore,
                    evCostScore,
                    incomeScore,
                    vtcExperienceScore,
                    drivingRecordScore,
                    Math.min(totalScore, MAX_SCORE)
            );
        });
    }

    private int calculatePersonalDataScore(Subscriber subscriber) {
        int score = 0;

        // Age
        int age = Period.between(subscriber.getDateNaissance(), LocalDate.now()).getYears();
        if (age >= 25 && age <= 50) score += 60;
        else if (age > 50) score += 40;
        else score += 20;

        // Residence
        if (subscriber.getAddress().getPaysNaissance().equalsIgnoreCase("Abidjan")) score += 60;
        else score += 30;

        // Nationality
        if (subscriber.getNationalite().equalsIgnoreCase("CI")) score += 60;
        else if (subscriber.getNationalite().startsWith("CEDEAO")) score += 40;
        else score += 20;

        // Marital status
        if (subscriber.getSituationMatrimoniale() == SituationMatrimoniale.MARIE) score += 60;
        else score += 30;

        // Number of children
        int children = subscriber.getNombreEnfants();
        if (children >= 2) score += 57;
        else if (children == 1) score += 40;
        else score += 20;

        return Math.min(score, PERSONAL_DATA_MAX);
    }

    private int calculateEvCostScore(Subscriber subscriber) {
        // Note: EV cost is not present in the current Subscriber model
        // This is a placeholder calculation. You should add a field for selected vehicle cost
        double evCost = 10_000_000; // Placeholder value
        if (evCost <= 10_000_000) return EV_COST_MAX;
        else if (evCost <= 15_000_000) return (int) (EV_COST_MAX * 0.8);
        else if (evCost <= 20_000_000) return (int) (EV_COST_MAX * 0.6);
        else return (int) (EV_COST_MAX * 0.4);
    }

    private int calculateIncomeScore(Subscriber subscriber) {
        BigDecimal totalMonthlyIncome = subscriber.getRevenuMensuel()
                .add(subscriber.getRevenuesConjoint());

        if (totalMonthlyIncome.compareTo(new BigDecimal("1000000")) >= 0) return INCOME_MAX;
        else if (totalMonthlyIncome.compareTo(new BigDecimal("750000")) >= 0) return (int) (INCOME_MAX * 0.8);
        else if (totalMonthlyIncome.compareTo(new BigDecimal("500000")) >= 0) return (int) (INCOME_MAX * 0.6);
        else return (int) (INCOME_MAX * 0.4);
    }

    private int calculateVtcExperienceScore(Subscriber subscriber) {
        int score = 0;

        // VTC experience
        long experienceMonthsLong = Period.between(subscriber.getDateDebutExerciceVTC(), LocalDate.now()).toTotalMonths();
        int experienceMonths = (int) Math.min(experienceMonthsLong, Integer.MAX_VALUE);

        if (experienceMonths >= 24) score += 150;
        else if (experienceMonths >= 12) score += 100;
        else if (experienceMonths >= 6) score += 50;

        return Math.min(score, VTC_EXPERIENCE_MAX);
    }

    private int calculateDrivingRecordScore(Subscriber subscriber) {
        int score = 0;

        // Driving license age
        int licenseAgeYears = Period.between(subscriber.getDateObtentionPermis(), LocalDate.now()).getYears();
        if (licenseAgeYears >= 5) score += 30;
        else if (licenseAgeYears >= 3) score += 20;
        else score += 10;

        // Driving license points, recent accidents, and outstanding fines are not present in the current Subscriber model
        // These are placeholder calculations. You should add fields for these in the Subscriber model
        int licensePoints = 12; // Placeholder value
        if (licensePoints >= 10) score += 25;
        else if (licensePoints >= 6) score += 15;
        else score += 5;

        int accidents = 0; // Placeholder value
        if (accidents == 0) score += 15;
        else if (accidents == 1) score += 5;

        boolean hasFines = false; // Placeholder value
        if (!hasFines) score += 15;

        return Math.min(score, DRIVING_RECORD_MAX);
    }
}