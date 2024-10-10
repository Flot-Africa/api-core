package africa.flot.presentation.mapper;

import africa.flot.application.command.CreateSubscriberCommand;
import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.enums.SubscriberStatus;
import africa.flot.domain.model.valueobject.Address;
import africa.flot.presentation.dto.command.CreateSubscriberDTO;
import africa.flot.presentation.dto.query.SubscriberDTO;
import africa.flot.presentation.dto.query.AddressDTO;
import org.mapstruct.*;

@Mapper(componentModel = "cdi")
public interface SubscriberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "LEADS")
    @Mapping(target = "kycDocuments", ignore = true)
    @Mapping(target = "evaluation", ignore = true)
    @Mapping(target = "account", ignore = true)
    Subscriber toEntity(CreateSubscriberCommand command);

    @Mapping(target = "firstName", source = "prenom")
    @Mapping(target = "lastName", source = "nom")
    @Mapping(target = "phone", source = "telephone")
    @Mapping(target = "driverLicenseNumber", source = "numeroCNI")
    @Mapping(target = "address", source = "adresse")
    CreateSubscriberDTO toDTO(CreateSubscriberCommand command);

    @Mapping(target = "firstName", source = "prenom")
    @Mapping(target = "lastName", source = "nom")
    @Mapping(target = "phone", source = "telephone")
    @Mapping(target = "driverLicenseNumber", source = "numeroCNI")
    @Mapping(target = "address", source = "adresse")
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "kybStatus", ignore = true)
    @Mapping(target = "creditScore", ignore = true)
    @Mapping(target = "yangoId", ignore = true)
    @Mapping(target = "uberId", ignore = true)
    SubscriberDTO toDTO(Subscriber entity);

    @Mapping(target = "prenom", source = "firstName")
    @Mapping(target = "nom", source = "lastName")
    @Mapping(target = "telephone", source = "phone")
    @Mapping(target = "numeroCNI", source = "driverLicenseNumber")
    @Mapping(target = "adresse", source = "address")
    CreateSubscriberCommand toCommand(CreateSubscriberDTO dto);

    AddressDTO addressToDTO(Address address);

    @Mapping(target = "state", ignore = true)
    Address dtoToAddress(AddressDTO dto);

    @AfterMapping
    default void setDefaultValues(@MappingTarget Subscriber subscriber) {
        subscriber.status = SubscriberStatus.LEADS;
    }
}