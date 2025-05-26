package africa.flot.domain.model;

import lombok.Data;

/**
 * Données obtenues de l'API Yango pour évaluer les conducteurs
 */
@Data
public class YangoDriverData {
    private int totalRides;
    private int experienceYears;
    private double monthlyRevenue;
    private double rating;
    private int accidents;
    private double drivingTestScore;
}