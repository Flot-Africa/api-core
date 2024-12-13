/*
package africa.flot.application.usecase.lead;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.application.exceptions.LeadNotFoundException;
import africa.flot.application.ports.LeadRepositoryPort;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.valueobject.AddressMapper;
import africa.flot.infrastructure.service.JetfySmsService;
import africa.flot.infrastructure.util.PasswordGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class CreatFineractClient {

    @Inject
    JetfySmsService smsService;

    private final LeadRepositoryPort leadRepositoryPort;

    public CreatFineractClient(LeadRepositoryPort leadRepositoryPort) {
        this.leadRepositoryPort = leadRepositoryPort;
    }

    public Uni<Lead> createLead(CreateFeneratClientCommande commande) {
        return Uni.createFrom().item(() -> {
                    // Vérification de l'email
                    this.leadExisteByEmail(commande.getEmailAddress());
                    // Génération du lead

                    return this.generate(commande);
                })
                .flatMap(lead -> {
                    // Génération des credentials
                    String generatedPassword = PasswordGenerator.generate();
                    String username = formatPhoneNumber(lead.getPhoneNumber());
                    String message = String.format(
                            "Bienvenue chez Flot! Vos identifiants de connexion sont:\nIdentifiant: %s\nMot de passe: %s",
                            username,
                            generatedPassword
                    );

                    // Envoi du SMS et persistance
                    return sendSmsAndPersistLead(lead, username, message);
                });
    }

    private Lead generate(CreateFeneratClientCommande commande) {
        UUID id = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(id);

        // Gestion du nom
        if (commande.getFirstname() != null && commande.getLastname() != null) {
            lead.setFirstName(commande.getFirstname());
            lead.setLastName(commande.getLastname());
        } else {
            lead.setFullname(commande.getFullname());
        }

        // Autres informations de base
        lead.setMiddlename(commande.getMiddlename());
        lead.setActive(commande.getActive());
        lead.setLocale(commande.getLocale());
        lead.setDateFormat(commande.getDateFormat());

        // Gestion de la date d'activation
        if (Boolean.TRUE.equals(commande.getActive())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "dd MMMM yyyy",
                    Locale.forLanguageTag(commande.getLocale())
            );
            lead.setActivationDate(LocalDate.parse(
                    commande.getActivationDate().format(formatter)
            ));
        }

        // Informations client
        lead.setGroupId(commande.getGroupId());
        lead.setGenderId(commande.getGenderId());
        lead.setExternalId(commande.getExternalId());
        lead.setAccountNo(commande.getAccountNo());
        lead.setStaffId(commande.getStaffId());
        lead.setMobileNo(commande.getMobileNo());
        lead.setSavingsProductId(commande.getSavingsProductId());
        lead.setClientTypeId(commande.getClientTypeId());
        lead.setClientClassificationId(commande.getClientClassificationId());
        lead.setLegalFormId(commande.getLegalFormId());
        lead.setBirthDate(commande.getDateOfBirth());
        lead.setEmail(commande.getEmailAddress());

        // Gestion des adresses
        if (commande.getAddress() != null && !commande.getAddress().isEmpty()) {
            lead.setFineractAddresses(AddressMapper.toEntityList(commande.getAddress()));
        } else {
            lead.convertMainAddressToFineract();
        }

        return lead;
    }

    private Uni<Lead> sendSmsAndPersistLead(Lead lead, String phoneNumber, String message) {
        return smsService.sendSMS(phoneNumber, message)
                .flatMap(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        return lead.<Lead>persistAndFlush();
                    }
                    return Uni.createFrom().failure(
                            new RuntimeException("Échec de l'envoi du SMS")
                    );
                });
    }

    private void leadExisteByEmail(String emailAddress) {
        boolean isPresent = leadRepositoryPort.findByEmail(emailAddress);
        if (isPresent) {
            throw new LeadNotFoundException(
                    "L'adresse Email : " + emailAddress + " existe déjà"
            );
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        return phoneNumber.length() > 3 ? phoneNumber.substring(3) : phoneNumber;
    }
}*/
