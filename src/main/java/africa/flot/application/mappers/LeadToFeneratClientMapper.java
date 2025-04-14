package africa.flot.application.mappers;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.enums.LeadStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class LeadToFeneratClientMapper {

    public static CreateFeneratClientCommande toCommand(Lead lead) {
        if (lead == null) {
            return null;
        }

        CreateFeneratClientCommande command = new CreateFeneratClientCommande();

        // Handle dates with French locale and format
        DateTimeFormatter frenchFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

        // Map basic identity fields
        command.setExternalId(String.valueOf(lead.getId()));
        command.setFirstname(lead.getFirstName());
        command.setLastname(lead.getLastName());
        command.setOfficeId(6L);
        command.setMobileNo(lead.getPhoneNumber());
        command.setEmailAddress(lead.getEmail());
        command.setStaffId(2L);
        command.setIsStaff(false);
        command.setLegalFormId(1L);

        // Set locale and date format with defaults
        command.setLocale("fr");
        command.setDateFormat("dd MMMM yyyy");
        command.setActive(lead.getStatus() == LeadStatus.VALIDEE);
        if (lead.getStatus() == LeadStatus.VALIDEE) {
            command.setActive(true);
            command.setActivationDate(LocalDateTime.now().format(frenchFormatter));
        }else {
            command.setActive(false);
        }


        // Format date of birth
        if (lead.getBirthDate() != null) {
            command.setDateOfBirth(lead.getBirthDate().format(frenchFormatter));
        }

        // Format submission date
        if (lead.getCreatedAt() != null) {
            command.setSubmittedOnDate(lead.getCreatedAt().format(frenchFormatter));
        } else {
            // Default to current date if not provided
            command.setSubmittedOnDate(LocalDateTime.now().format(frenchFormatter));
        }

        // Map family members if they exist

        command.setFamilyMembers(new ArrayList<>());

        return command;
    }
}