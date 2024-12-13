package africa.flot.domain.model.valueobject;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class FineractAddress {
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private Long addressTypeId;
    private String city;
    private Long countryId;
    private Boolean isActive = true;
    private Long postalCode;
    private Long stateProvinceId;
    private String street;
}