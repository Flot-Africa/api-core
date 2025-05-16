package africa.flot.infrastructure.service;

import africa.flot.application.mappers.LeadToFeneratClientMapper;
import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.application.service.FlotLoanService;
import africa.flot.application.dto.command.CreateLoanCommand;
import africa.flot.domain.model.*;
import africa.flot.infrastructure.util.PasswordGenerator;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FlotClientServiceImpl {
    private static final Logger LOG = Logger.getLogger(FlotClientServiceImpl.class);

    @Inject
    FlotLoanService flotLoanService;

    @Inject
    JetfySmsService smsService;

    @WithSession
    public Uni<Response> createClient(InitLoanCommande commande) {
        LOG.infof("Création d'un client Flot pour lead %s, véhicule %s",
                commande.getLeadId(), commande.getVehicleId());

        return Lead.<Lead>findById(commande.getLeadId())
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Lead introuvable : " + commande.getLeadId())
                )
                .flatMap(lead -> Vehicle.<Vehicle>findById(commande.getVehicleId())
                        .onItem().ifNull().failWith(() ->
                                new NotFoundException("Véhicule introuvable : " + commande.getVehicleId())
                        )
                        .flatMap(vehicle -> {
                            // Créer la commande de prêt
                            CreateLoanCommand loanCommand = new CreateLoanCommand();
                            loanCommand.setLeadId(lead.getId());
                            loanCommand.setVehicleId(vehicle.getId());

                            // Créer le prêt
                            return flotLoanService.createLoan(loanCommand)
                                    .flatMap(loan -> {
                                        // Créer le compte utilisateur
                                        String generatedPassword = PasswordGenerator.generate();
                                        String clientUsername = formatPhoneNumber(lead.getPhoneNumber());

                                        Account account = new Account();
                                        account.setLead(lead);
                                        account.setUsername(clientUsername);
                                        account.setPasswordHash(BcryptUtil.bcryptHash(generatedPassword));
                                        account.setTemporaryPassword(BcryptUtil.bcryptHash(generatedPassword));
                                        account.setActive(true);
                                        account.setPasswordChanged(false);

                                        return account.<Account>persistAndFlush()
                                                .flatMap(savedAccount ->
                                                        sendWelcomeSms(clientUsername, generatedPassword, account, lead)
                                                                .map(smsResp -> {
                                                                    LOG.infof("Client créé avec succès: Lead %s, Prêt %s, Compte %s",
                                                                            lead.getId(), loan.getId(), account.getId());
                                                                    return Response.ok()
                                                                            .entity(new ClientCreationResponse(
                                                                                    loan.getId(),
                                                                                    account.getId(),
                                                                                    "Client et prêt créés avec succès"
                                                                            ))
                                                                            .build();
                                                                })
                                                                .onFailure().recoverWithItem(error -> {
                                                                    LOG.error("Échec envoi SMS, mais Client + Prêt + Compte OK", error);

                                                                    // Marquer le SMS comme en attente
                                                                    account.setPendingWelcomeSms(true);
                                                                    account.setTemporaryPassword(generatedPassword);
                                                                    account.setSmsRetryCount(0);
                                                                    account.persistAndFlush().await().indefinitely();

                                                                    return Response.ok()
                                                                            .entity(new ClientCreationResponse(
                                                                                    loan.getId(),
                                                                                    account.getId(),
                                                                                    "Client et prêt créés, SMS en attente de réessai"
                                                                            ))
                                                                            .build();
                                                                })
                                                );
                                    });
                        })
                )
                .onFailure().invoke(err -> LOG.error("Erreur createClient()", err));
    }

    private Uni<Response> sendWelcomeSms(String clientUsername, String password, Account account, Lead lead) {
        String message = String.format(
                "👋 Bienvenue %s %s chez FLOT Mobility!\n\n" +
                        "🔑 Vos identifiants FLOT:\n" +
                        "Identifiant: %s\n" +
                        "Mot de passe: %s\n\n" +
                        "📱 Pour commencer votre expérience FLOT:\n" +
                        "Téléchargez notre application:\n" +
                        "https://flot.africa\n\n" +
                        "⚠️ Pour votre sécurité, changez votre mot de passe à la première connexion\n" +
                        "📞 Service client: 0779635252",
                lead.getFirstName(),
                lead.getLastName(),
                clientUsername,
                password
        );

        return smsService.sendSMS(lead.getPhoneNumber(), message, account);
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 3) {
            return phoneNumber;
        }
        return phoneNumber.substring(3);
    }

    // Classe de réponse pour la création de client
    public static class ClientCreationResponse {
        public final java.util.UUID loanId;
        public final java.util.UUID accountId;
        public final String message;

        public ClientCreationResponse(java.util.UUID loanId, java.util.UUID accountId, String message) {
            this.loanId = loanId;
            this.accountId = accountId;
            this.message = message;
        }
    }
}