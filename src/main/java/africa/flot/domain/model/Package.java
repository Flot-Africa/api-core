package africa.flot.domain.model;

import africa.flot.application.dto.query.PackageDTO;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "packages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    @OneToMany(mappedBy = "subscribedPackage", fetch = FetchType.EAGER)
    private List<Account> accounts;

    // Ajoutez ceci dans la classe Package
    public PackageDTO toDTO() {
        PackageDTO dto = new PackageDTO();
        dto.setId(this.id);
        dto.setName(this.name);
        dto.setDescription(this.description);
        dto.setWeeklyPayment(this.weeklyPayment);
        dto.setContractDuration(this.contractDuration);
        dto.setWeeklyMileageLimit(this.weeklyMileageLimit);
        dto.setMaintenanceIncluded(this.maintenanceIncluded);
        dto.setInsuranceIncluded(this.insuranceIncluded);
        dto.setExcessMileageFee(this.excessMileageFee);
        dto.setRoadsideAssistance(this.roadsideAssistance);
        dto.setChargingAccess(this.chargingAccess);
        dto.setPurchaseOption(this.purchaseOption);
        dto.setRewardProgram(this.rewardProgram);
        return dto;
    }

}
