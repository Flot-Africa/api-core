package africa.flot.application.mappers;

import africa.flot.application.dto.command.AddressDTO;
import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.valueobject.FineractAddress;

import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Collectors;

public class LeadToFeneratClientMapper {

    public static CreateFeneratClientCommande toCommand(Lead lead) {
        if (lead == null) {
            return null;
        }

        CreateFeneratClientCommande command = new CreateFeneratClientCommande();

        command.setExternalId(String.valueOf(lead.getId()));
        command.setFirstname(lead.getFirstName());
        command.setLastname(lead.getLastName());
        command.setMiddlename(lead.getMiddlename());
        command.setMobileNo(lead.getMobileNo());
        command.setEmailAddress(lead.getEmail());
        command.setActive(lead.getActive());
        command.setActivationDate(lead.getActivationDate());
        command.setDateFormat(lead.getDateFormat());
        command.setLocale(lead.getLocale());
        command.setGroupId(lead.getGroupId());
        command.setAccountNo(lead.getAccountNo());
        command.setStaffId(lead.getStaffId());
        command.setSavingsProductId(lead.getSavingsProductId());
        command.setGenderId(lead.getGenderId());
        command.setClientTypeId(lead.getClientTypeId());
        command.setClientClassificationId(lead.getClientClassificationId());
        command.setLegalFormId(lead.getLegalFormId());

        // => Ajouter ici l'officeId :
        command.setOfficeId(lead.getOfficeId());  // <-- SI lead a un officeId

        // Convert LocalDate -> Date si besoin
        if (lead.getBirthDate() != null) {
            command.setDateOfBirth(Date.from(
                    lead.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            ));
        }

        // Adresses (si présent)
        if (lead.getFineractAddresses() != null && !lead.getFineractAddresses().isEmpty()) {
            command.setAddress(lead.getFineractAddresses().stream()
                    .map(LeadToFeneratClientMapper::mapToAddressDTO)
                    .collect(Collectors.toList()));
        }

        return command;
    }


    private static AddressDTO mapToAddressDTO(FineractAddress fineractAddress) {
        // Note: You'll need to create and implement the AddressDTO class
        // based on your specific requirements
        // Map the fields from FineractAddress to AddressDTO
        // Implement this mapping based on your AddressDTO structure
        return new AddressDTO();
    }
}