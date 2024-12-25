package africa.flot.application.mappers;

import africa.flot.application.dto.command.AddressDTO;
import africa.flot.domain.model.valueobject.Address;



public class AddressMappers {

    public  static AddressDTO toAddress(Address address) {
        if (address == null) {
            return null;
        }
        AddressDTO dto = new AddressDTO();
        dto.setAddressLine1(address.getAddressLine1());
        dto.setAddressLine2(address.getAddressLine2());
        dto.setCity(address.getCity());
        dto.setAddressLine3(address.getAddressLine3());
        dto.setPostalCode(address.getPostalCode());
        dto.setAddressTypeId(address.getAddressTypeId());
        dto.setCountryId(address.getCountryId());
        dto.setIsActive(address.getIsActive());
        dto.setStreet(address.getStreet());
        dto.setStateProvinceId(address.getStateProvinceId());

        return dto;
    }
}
