package africa.flot.application.command;

import africa.flot.domain.model.valueobject.Address;

public record CreateSubscriberCommand(
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        String driverLicenseNumber,
        Address address) {

}