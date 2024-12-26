package africa.flot.infrastructure.service;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.application.mappers.LeadToFeneratClientMapper;
import africa.flot.domain.model.Account;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.Vehicle;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.client.FineractClient;
import africa.flot.infrastructure.util.PasswordGenerator;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@ApplicationScoped
public class FeneractServiceClientImpl {
    private static final Logger LOG = Logger.getLogger(FeneractServiceClientImpl.class);

    @Inject
    @RestClient
    FineractClient fineractClient;

    @Inject
    LoanService loanService;

    @Inject
    JetfySmsService smsService;

    @PostConstruct
    void init() {
        LOG.info("FenerateServiceClientImpl initialisé avec client non-bloquant.");
    }

    @WithSession
    public Uni<Response> createClient(InitLoanCommande commande) {
        return Lead.<Lead>find("""
                                select l from Lead l
                                where l.id = :id
                        """, Parameters.with("id", commande.getLeadId()))
                .project(Lead.class)
                .firstResult()
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Lead introuvable : " + commande.getLeadId())
                )
                .flatMap(lead -> Vehicle.<Vehicle>find("""
                                select v from Vehicle v
                                where v.id = :id
                        """, Parameters.with("id", commande.getVehicleId()))
                        .project(Vehicle.class)
                        .firstResult()
                        .onItem().ifNull().failWith(() ->
                                new NotFoundException("Véhicule introuvable : " + commande.getVehicleId())
                        )
                        .flatMap(vehicle -> {
                            // Mapper le Lead vers la commande pour Fineract
                            CreateFeneratClientCommande cmd = LeadToFeneratClientMapper.toCommand(lead);
                            validateCommand(cmd);

                            // Créer le payload pour l'appel Fineract
                            JsonObject payload = createFineractRequest(cmd);

                            // Appeler le client Fineract pour créer le client
                            return fineractClient.createClient(payload)
                                    .onItem().invoke(resp -> {
                                        LOG.info("Réponse createClient -> HTTP " + resp.getStatus());
                                    })
                                    .flatMap(resp -> {
                                        if (resp.getStatus() < 200 || resp.getStatus() >= 300) {
                                            return Uni.createFrom().failure(
                                                    new BusinessException("Échec création client Fineract, HTTP=" + resp.getStatus())
                                            );
                                        }

                                        // Extraire le clientId de la réponse
                                        JsonObject json = JsonObject.mapFrom(resp.readEntity(Map.class));
                                        Integer clientId = json.getInteger("clientId");
                                        if (clientId == null) {
                                            return Uni.createFrom().failure(
                                                    new BusinessException("Impossible de lire clientId dans la réponse Fineract.")
                                            );
                                        }

                                        // Créer un prêt pour le client
                                        return loanService.createLoan(clientId, commande.getProduitId(), vehicle.price, String.valueOf(lead.getId()))
                                                .map(loanResp -> Map.of(
                                                        "loanResponse", loanResp,
                                                        "lead", lead,
                                                        "clientId", clientId
                                                ));
                                    });
                        }))
                .flatMap(data -> {
                    Response loanResp = (Response) data.get("loanResponse");
                    Lead lead = (Lead) data.get("lead");
                    Integer clientId = (Integer) data.get("clientId");

                    // Vérifier si le prêt a été créé avec succès
                    if (loanResp.getStatus() == Response.Status.OK.getStatusCode()) {
                        // Générer un mot de passe pour le compte utilisateur
                        String generatedPassword = PasswordGenerator.generate();
                        String clientUsername = formatPhoneNumber(lead.getPhoneNumber());

                        Account account = new Account();
                        account.setLead(lead);
                        account.setUsername(clientUsername);
                        account.setPasswordHash(BcryptUtil.bcryptHash(generatedPassword));
                        account.setActive(true);
                        account.setFineractClientId(clientId);

                        // Persister le compte et envoyer un SMS de bienvenue
                        return account.<Account>persistAndFlush()
                                .flatMap(savedAccount ->
                                        sendWelcomeSms(clientUsername, generatedPassword, account, lead)
                                                .map(smsResp -> Response.ok("Client + Prêt créés + SMS envoyé").build())
                                                .onFailure().recoverWithItem(error -> {
                                                    LOG.error("Échec envoi SMS, mais Client + Prêt + Compte OK", error);
                                                    return Response.ok("Client + Prêt + Compte créés, SMS échoué").build();
                                                })
                                );
                    } else {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity("Échec création du Prêt")
                                        .build()
                        );
                    }
                })
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
        return smsService.sendSMS(lead.getPhoneNumber(), message, account)
                .onFailure().recoverWithItem(error -> {
                    LOG.error("Échec envoi SMS initial", error);
                    account.setPendingWelcomeSms(true);
                    account.setTemporaryPassword(password);
                    account.setSmsRetryCount(0);
                    return account.<Account>persistAndFlush()
                            .map(savedAccount -> Response.ok()
                                    .entity("Client + Prêt + Compte créés, SMS en attente de réessai")
                                    .build())
                            .await().indefinitely();
                });
    }

    private JsonObject createFineractRequest(CreateFeneratClientCommande cmd) {
        Map<String, Object> request = new HashMap<>();

        // Méthode utilitaire pour éviter la répétition des if != null
        BiConsumer<String, Object> putIfNotNull = (key, value) -> {
            if (value != null) {
                request.put(key, value);
            }
        };

        // Utilisation de la méthode putIfNotNull pour les champs optionnels
        putIfNotNull.accept("firstname", cmd.getFirstname());
        putIfNotNull.accept("lastname", cmd.getLastname());
        putIfNotNull.accept("externalId", cmd.getExternalId());
        // On formate le mobile s’il n’est pas null
        putIfNotNull.accept("mobileNo",
                cmd.getMobileNo() != null ? formatPhoneNumber(cmd.getMobileNo()) : null
        );
        putIfNotNull.accept("emailAddress", cmd.getEmailAddress());
        putIfNotNull.accept("dateOfBirth", cmd.getDateOfBirth());
        putIfNotNull.accept("submittedOnDate", cmd.getSubmittedOnDate());

        // Les champs obligatoires ou par défaut
        request.put("officeId", cmd.getOfficeId());
        request.put("active", cmd.getActive());
        request.put("legalFormId", cmd.getLegalFormId());
        request.put("locale", cmd.getLocale() != null ? cmd.getLocale() : "fr");
        request.put("dateFormat", cmd.getDateFormat() != null ? cmd.getDateFormat() : "dd MMMM yyyy");
        request.put("isStaff", cmd.getIsStaff());
        request.put("staffId", cmd.getStaffId());

        // Cas particulier: on ne met l’activationDate que si active = true et activationDate non null
        if (cmd.getActive() && cmd.getActivationDate() != null) {
            request.put("activationDate", cmd.getActivationDate());
        }

        // Famille
        if (cmd.getFamilyMembers() != null && !cmd.getFamilyMembers().isEmpty()) {
            request.put("familyMembers", cmd.getFamilyMembers());
        }

        return JsonObject.mapFrom(request);
    }

    private void validateCommand(CreateFeneratClientCommande cmd) {
        List<String> errors = new ArrayList<>();

        // Vérifie les conditions d'erreur et ajoute un message le cas échéant
        checkCondition(errors,
                cmd.getFirstname() == null || cmd.getFirstname().isBlank(),
                "firstname est obligatoire");

        checkCondition(errors,
                cmd.getLastname() == null || cmd.getLastname().isBlank(),
                "lastname est obligatoire");

        checkCondition(errors,
                cmd.getOfficeId() == 0,
                "officeId est obligatoire");

        checkCondition(errors,
                cmd.getActive() && cmd.getActivationDate() == null,
                "activationDate est obligatoire quand active=true");

        // S'il y a des erreurs accumulées, on lève une BusinessException
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(", ", errors));
        }
    }

    /**
     * Ajoute un message d'erreur à la liste si la condition est vraie.
     */
    private void checkCondition(List<String> errors, boolean condition, String errorMessage) {
        if (condition) {
            errors.add(errorMessage);
        }
    }


    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 3) {
            return phoneNumber;
        }
        return phoneNumber.substring(3);
    }
}