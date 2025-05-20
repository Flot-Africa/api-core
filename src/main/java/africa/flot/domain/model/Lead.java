package africa.flot.domain.model;

import africa.flot.domain.model.enums.Gender;
import africa.flot.domain.model.enums.HousingStatus;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.valueobject.Address;
import africa.flot.domain.model.enums.LeadStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
public class Lead extends PanacheEntityBase {
    @Id
    private UUID id;

    @Column(name = "key_form")
    private UUID keyForm;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "office_id")
    private Integer officeId;

    @Column(name = "fullname")
    private String fullname;

    @Column(name = "middlename")
    private String middlename;

    @Column(name = "activationDate")
    private String activationDate;

    @Column(name = "active")
    private Boolean active = false;

    @Column(name = "dateFormat")
    private String dateFormat;

    @Column(name = "locale")
    private String locale;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private LeadStatus status;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(unique = true)
    private String email;

    @Column(name = "permit_acquisition_date")
    private LocalDate permitAcquisitionDate;

    @Column(name = "vtc_start_date")
    private LocalDate vtcStartDate;

    @Column(name = "salary")
    private BigDecimal salary;

    @Column(name = "expenses")
    private BigDecimal expenses;

    @Column(name = "marital_status")
    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;

    @Column(name = "housing_status")
    @Enumerated(EnumType.STRING)
    private HousingStatus housingStatus;

    @Column(name = "spouse_income")
    private BigDecimal spouseIncome;

    @Column(name = "children_count")
    private Integer childrenCount;

    // Existing fields for driving records & VTC info
    @Column(name = "license_points")
    private Integer licensePoints; // Remaining points on the license (out of 12)

    @Column(name = "current_infractions_count")
    private Integer currentInfractionsCount; // Number of current infractions

    @Column(name = "total_fine_amount", precision = 15, scale = 2)
    private BigDecimal totalFineAmount; // Total fine amount in francs for current infractions

    @Column(name = "accident_count")
    private Integer accidentCount; // Number of recent accidents

    @Column(name = "has_electric_vehicle_experience")
    private Boolean hasElectricVehicleExperience; // Nouvel attribut

    @Column(name = "average_vtc_rating")
    private Double averageVtcRating; // Note moyenne sur les plateformes VTC

    @Column(name = "historical_infractions_count")
    private Integer historicalInfractionsCount; // Nombre total d'infractions historiques

    // New fields for scoring V2
    @Column(name = "education_level")
    private String educationLevel; // Niveau d'études (BAC, LICENCE, MASTER, etc.)

    @Column(name = "vtc_rides_count")
    private Integer vtcRidesCount; // Nombre total de courses VTC effectuées

    @Column(name = "vtc_monthly_income")
    private BigDecimal vtcMonthlyIncome; // Revenu mensuel des activités VTC

    @Column(name = "driving_test_score")
    private Double drivingTestScore; // Score du test de conduite

    @Column(name = "informal_income")
    private BigDecimal informalIncome; // Revenus informels

    @Column(name = "debt_amount")
    private BigDecimal debtAmount; // Montant de la dette actuelle

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Embedded
    private Address address;  // Adresse principale existante
}