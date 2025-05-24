package africa.flot.domain.model;

import africa.flot.domain.model.enums.Gender;
import africa.flot.domain.model.enums.HousingStatus;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.enums.LeadStatus;
import africa.flot.domain.model.valueobject.Address;
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

    //Identité et Informations Personnelles
    @Id
    private UUID id;

    @Column(name = "key_form")
    private UUID keyForm;

    @Column(name = "fullname")
    private String fullname;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "middlename")
    private String middlename;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_place")
    private String birthPlace;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Column(name = "children_count")
    private Integer childrenCount;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "revenueInformel")
    private BigDecimal revenueInformel;

    @Column(name = "endettement")
    private BigDecimal endettement;


    @Column(name = "nombreDeCourse")
    private int nombreDeCourse;


    @Column(name = "revenueYango")
    private BigDecimal revenueYango;


    // 📞 Contact & Localisation
    @Column(name = "phone_number")
    private String phoneNumber;

    @Embedded
    private Address address;

    @Column(name = "locale")
    private String locale;

    @Column(name = "dateFormat")
    private String dateFormat;

    @Column(name = "office_id")
    private Integer officeId;

    // 📅 Activité & État
    @Column(name = "active")
    private Boolean active = false;

    @Column(name = "activationDate")
    private String activationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LeadStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 💰 Situation Financière
    @Column(name = "salary")
    private BigDecimal salary;

    @Column(name = "expenses")
    private BigDecimal expenses;

    @Column(name = "spouse_income")
    private BigDecimal spouseIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_status")
    private HousingStatus housingStatus;

    @Column(name = "informal_income")
    private BigDecimal informalIncome;

    @Column(name = "debt_amount")
    private BigDecimal debtAmount;

    // 🚗 Informations Permis & Conduite
    @Column(name = "permit_acquisition_date")
    private LocalDate permitAcquisitionDate;

    @Column(name = "license_points")
    private Integer licensePoints;

    @Column(name = "current_infractions_count")
    private Integer currentInfractionsCount;

    @Column(name = "historical_infractions_count")
    private Integer historicalInfractionsCount;

    @Column(name = "total_fine_amount", precision = 15, scale = 2)
    private BigDecimal totalFineAmount;

    @Column(name = "accident_count")
    private Integer accidentCount;

    @Column(name = "driving_test_score")
    private Double drivingTestScore;

    // 🚖 Activité VTC
    @Column(name = "vtc_start_date")
    private LocalDate vtcStartDate;

    @Column(name = "has_electric_vehicle_experience")
    private Boolean hasElectricVehicleExperience;

    @Column(name = "average_vtc_rating")
    private Double averageVtcRating;

    @Column(name = "vtc_rides_count")
    private Integer vtcRidesCount;

    @Column(name = "vtc_monthly_income")
    private BigDecimal vtcMonthlyIncome;
}
