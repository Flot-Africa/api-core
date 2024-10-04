package africa.flot.presentation.dto.query;

import africa.flot.domain.model.enums.KYBStatus;
import java.util.UUID;

public class SubscriberDTO {
    public UUID id;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String driverLicenseNumber;
    public AddressDTO address;
    public boolean isActive;
    public KYBStatus kybStatus;
    public Double creditScore;
    public String yangoId;
    public String uberId;
}

