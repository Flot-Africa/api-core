package africa.flot.domain.model;

import africa.flot.domain.model.enums.Gender;
import africa.flot.domain.model.enums.HousingStatus;
import africa.flot.domain.model.enums.MaritalStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
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

    @Column(name = "address")
    private String address;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
