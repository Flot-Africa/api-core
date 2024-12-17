package africa.flot.domain.model;

import africa.flot.domain.model.enums.Gender;
import africa.flot.domain.model.enums.HousingStatus;
import africa.flot.domain.model.enums.MaritalStatus;
import africa.flot.domain.model.valueobject.Address;
import africa.flot.domain.model.valueobject.FineractAddress;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    @Column(name = "fullname")
    private String fullname;

    @Column(name = "middlename")
    private String middlename;

    @Column(name = "activationDate")
    private Integer activationDate = null;

    @Column(name = "active")
    private Boolean active = false;

    @Column(name = "dateFormat")
    private String dateFormat;

    @Column(name = "locale")
    private String locale;

    private Long groupId;
    private String externalId;
    private String accountNo;
    private Long staffId;
    private String mobileNo;
    private Long savingsProductId;
    private Long genderId;
    private Long clientTypeId;
    private Long clientClassificationId;
    private Long legalFormId;

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

    // New fields added below
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Embedded
    private Address address;  // Adresse principale existante

    @ElementCollection
    @CollectionTable(
            name = "lead_fineract_addresses",
            joinColumns = @JoinColumn(name = "lead_id")
    )
    private List<FineractAddress> fineractAddresses = new ArrayList<>();

    // Méthode utilitaire pour convertir l'adresse principale en adresse Fineract
    public void convertMainAddressToFineract() {
        if (this.address != null) {
            FineractAddress fineractAddress = new FineractAddress();
            fineractAddress.setAddressLine1(this.address.getStreet());
            fineractAddress.setCity(this.address.getCity());
            fineractAddress.setPostalCode(Long.valueOf(this.address.getPostalCode()));
            fineractAddress.setCountryId(1L); // À adapter selon vos besoins
            fineractAddress.setStateProvinceId(1L); // À adapter selon vos besoins
            fineractAddress.setAddressTypeId(1L); // Type par défaut
            fineractAddress.setIsActive(true);

            if (this.fineractAddresses == null) {
                this.fineractAddresses = new ArrayList<>();
            }
            this.fineractAddresses.add(fineractAddress);
        }
    }

    // Méthode pour définir les adresses Fineract
    public void setFineractAddresses(List<FineractAddress> addresses) {
        if (addresses != null) {
            this.fineractAddresses = addresses;
        }
    }

    // Méthode pour obtenir les adresses au format Fineract
    public List<FineractAddress> getFineractAddresses() {
        if (this.fineractAddresses == null || this.fineractAddresses.isEmpty()) {
            convertMainAddressToFineract();
        }
        return this.fineractAddresses;
    }

}
