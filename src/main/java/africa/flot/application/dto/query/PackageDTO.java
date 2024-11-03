package africa.flot.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PackageDTO {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal weeklyPayment;
    private Integer contractDuration;
    private Integer weeklyMileageLimit;
    private Boolean maintenanceIncluded;
    private Boolean insuranceIncluded;
    private BigDecimal excessMileageFee;
    private Boolean roadsideAssistance;
    private Boolean chargingAccess;
    private Boolean purchaseOption;
    private Boolean rewardProgram;




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
