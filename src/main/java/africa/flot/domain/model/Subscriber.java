package africa.flot.domain.model;

import africa.flot.domain.model.enums.KYBStatus;
import africa.flot.domain.model.valueobject.Address;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@Table(name = "subscriber")
public class Subscriber extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name", nullable = false)
    public String lastName;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(name = "password_hash", nullable = false)
    public String password;

    @Column(nullable = false)
    public String phone;

    @Column(name = "driver_license_number", nullable = false, unique = true)
    public String driverLicenseNumber;

    @Embedded
    public Address address;

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyb_status", nullable = false)
    public KYBStatus kybStatus = KYBStatus.PENDING;

    @Column(name = "credit_score")
    public Double creditScore;

    @Column(name = "yango_id")
    public String yangoId;

    @Column(name = "uber_id")
    public String uberId;

    // Constructeurs, getters, setters

    public void verifyKYB() {
        this.kybStatus = KYBStatus.VERIFIED;
    }

    public void rejectKYB() {
        this.kybStatus = KYBStatus.REJECTED;
    }

    public void deactivate() {
        this.isActive = false;
    }
}