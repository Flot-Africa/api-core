package africa.flot.domain.model.valueobject;

import africa.flot.application.dto.command.AddressDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddressMapper {
    public static FineractAddress toEntity(AddressDTO dto) {
        if (dto == null) {
            return null;
        }

        FineractAddress address = new FineractAddress();
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setAddressLine3(dto.getAddressLine3());
        address.setAddressTypeId(dto.getAddressTypeId());
        address.setCity(dto.getCity());
        address.setCountryId(dto.getCountryId());
        address.setIsActive(dto.getIsActive());
        address.setPostalCode(dto.getPostalCode());
        address.setStateProvinceId(dto.getStateProvinceId());
        address.setStreet(dto.getStreet());

        return address;
    }

    public static List<FineractAddress> toEntityList(List<AddressDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(AddressMapper::toEntity)
                .collect(Collectors.toList());
    }
}
