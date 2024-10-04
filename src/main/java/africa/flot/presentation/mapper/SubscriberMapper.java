package africa.flot.presentation.mapper;

import africa.flot.application.command.CreateSubscriberCommand;
import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.enums.KYBStatus;
import africa.flot.domain.model.valueobject.Address;
import africa.flot.presentation.dto.command.CreateSubscriberDTO;
import africa.flot.presentation.dto.query.SubscriberDTO;
import org.mapstruct.*;

@Mapper(componentModel = "cdi")
public interface SubscriberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "kybStatus", ignore = true)
    @Mapping(target = "creditScore", ignore = true)
    @Mapping(target = "yangoId", ignore = true)
    @Mapping(target = "uberId", ignore = true)
    Subscriber toEntity(CreateSubscriberCommand command);

    CreateSubscriberCommand toCommand(CreateSubscriberDTO dto);

    SubscriberDTO toDTO(Subscriber entity);

    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "country", source = "address.country")
    africa.flot.presentation.dto.query.AddressDTO addressToDTO(Address address);

    @Mapping(target = "street", source = "street")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "postalCode", source = "postalCode")
    @Mapping(target = "country", source = "country")
    Address dtoToAddress(africa.flot.presentation.dto.command.AddressDTO dto);

    @AfterMapping
    default void setDefaultValues(@MappingTarget Subscriber subscriber) {
        subscriber.isActive = true;
        subscriber.kybStatus = KYBStatus.PENDING;
    }
}