package africa.flot.application.dto.command;

import africa.flot.domain.model.valueobject.Address;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class AddressDTO {

    private String addressLine1;
    private String addressLine2;
    private String addressLine3;

    @NotNull(message = "addressTypeId est obligatoire")
    private Long addressTypeId;

    private String city;

    @NotNull(message = "countryId est obligatoire")
    private Long countryId;

    private Boolean isActive = true;
    private Long postalCode;

    @NotNull(message = "stateProvinceId est obligatoire")
    private Long stateProvinceId;

    private String street;


    // Getters and setters
}
