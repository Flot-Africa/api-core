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
        command.setOfficeId(lead.getOfficeId());
        command.setMobileNo(lead.getPhoneNumber());
        command.setEmailAddress(lead.getEmail());

        // Set locale and date format with defaults
        command.setLocale("fr");
        command.setDateFormat("dd MMMM yyyy");
        command.setActive(lead.getStatus() == LeadStatus.ACTIVE);
        if (lead.getStatus() == LeadStatus.ACTIVE) {
            command.setActive(true);
            command.setActivationDate(LocalDateTime.now().format(frenchFormatter));
        }else {
            command.setActive(false);
        }

        // Map staff related fields
        command.setStaff(false);


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