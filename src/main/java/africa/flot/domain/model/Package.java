package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "packages")
@Data
public class Package extends PanacheEntityBase {
    @Id
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "weekly_payment", precision = 10, scale = 2, nullable = false)
    private BigDecimal weeklyPayment;

    @Column(name = "contract_duration", nullable = false)
    private Integer contractDuration;

    @Column(name = "weekly_mileage_limit", nullable = false)
    private Integer weeklyMileageLimit;

    @Column(name = "maintenance_included")
    private Boolean maintenanceIncluded;

    @Column(name = "insurance_included")
    private Boolean insuranceIncluded;

    @Column(name = "excess_mileage_fee", precision = 10, scale = 2)
    private BigDecimal excessMileageFee;

    @Column(name = "roadside_assistance")
    private Boolean roadsideAssistance;

    @Column(name = "charging_access")
    private Boolean chargingAccess;

    @Column(name = "purchase_option")
    private Boolean purchaseOption;

    @Column(name = "reward_program")
    private Boolean rewardProgram;

    @OneToMany(mappedBy = "subscribedPackage")
    private List<Account> accounts;
}
