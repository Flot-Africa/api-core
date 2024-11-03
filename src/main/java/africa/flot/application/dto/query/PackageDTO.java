package africa.flot.application.dto.query;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@RegisterForReflection
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

}

