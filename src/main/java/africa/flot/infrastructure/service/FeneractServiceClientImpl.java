package africa.flot.infrastructure.service;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.application.mappers.LeadToFeneratClientMapper;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.client.FineractClient;
import africa.flot.infrastructure.util.PasswordGenerator;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service permettant de créer un client Fineract, puis de créer un prêt,
 * et enfin d'envoyer un SMS de bienvenue, le tout en mode réactif (non bloquant).
 */
@ApplicationScoped
public class FeneractServiceClientImpl {

    private static final Logger LOG = Logger.getLogger(FeneractServiceClientImpl.class);

    @Inject
    @RestClient
    FineractClient fineractClient;

    @Inject
    LoanService loanService;

    @Inject
    JetfySmsService smsService; // <-- Réintroduit l'envoi de SMS

    @PostConstruct
    void init() {
        LOG.info("FenerateServiceClientImpl initialisé avec client non-bloquant.");
    }

    /**
     * Crée le client dans Fineract, puis un prêt, et enfin envoie un SMS de bienvenue.
     */
    @WithSession
    public Uni<Response> createClient(InitLoanCommande commande) {
        return Lead.<Lead>find("""
                            select l
                            from Lead l
                            where l.id = ?1
                        """, commande.getLeadId())

                .firstResult()
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Lead introuvable : " + commande.getLeadId())
                )
                .flatMap(lead -> {
                    // Transforme le Lead en commande
                    LOG.info("Lead : " + lead.getFirstName() + " " + lead.getLastName());
                    CreateFeneratClientCommande cmd = LeadToFeneratClientMapper.toCommand(lead);

                    validateCommand(cmd);

                    // Construit le JsonObject pour Fineract
                    JsonObject payload = createFineractRequest(cmd);

                    // 1) Appel réactif : POST /v1/clients
                    return fineractClient.createClient(payload)
                            .onItem().invoke(resp -> {
                                LOG.info("Réponse createClient -> HTTP " + resp.getStatus());
                            })
                            // 2) Création du prêt
                            .flatMap(resp -> {
                                if (resp.getStatus() < 200 || resp.getStatus() >= 300) {
                                    return Uni.createFrom().failure(
                                            new BusinessException("Échec création client Fineract, HTTP=" + resp.getStatus())
                                    );
                                }

                                // Lire "clientId" dans la réponse
                                JsonObject json = JsonObject.mapFrom(resp.readEntity(Map.class));
                                Integer clientId = json.getInteger("clientId");
                                if (clientId == null) {
                                    return Uni.createFrom().failure(
                                            new BusinessException("Impossible de lire clientId dans la réponse Fineract.")
                                    );
                                }

                                // Calcul du montant
                                BigDecimal loanAmount = calculateLoanAmount(lead);
                                // Appel createLoan
                                return loanService.createLoan(clientId, commande.getProduitId(), loanAmount)
                                        // On renvoie la réponse (Response) et le lead dans un tuple
                                        .map(loanResp -> Map.of(
                                                "loanResponse", loanResp,
                                                "lead", lead
                                        ));
                            })
                            // 3) Envoi du SMS si prêt créé OK
                            .flatMap(tuple -> {
                                Response loanResp = (Response) tuple.get("loanResponse");
                                Lead leadEntity = (Lead) tuple.get("lead");

                                if (loanResp.getStatus() == Response.Status.OK.getStatusCode()) {
                                    // Générer login et mot de passe
                                    String generatedPassword = PasswordGenerator.generate();
                                    String clientUsername = formatPhoneNumber(cmd.getMobileNo());

                                    // Envoyer un SMS de bienvenue
                                    return sendWelcomeSms(clientUsername, generatedPassword)
                                            // En cas de succès du SMS, on renvoie un 200
                                            .map(smsResp ->
                                                    Response.ok("Client + Prêt créés + SMS envoyé").build()
                                            )
                                            // En cas d'échec SMS, on log, et on renvoie quand même un OK
                                            .onFailure().recoverWithItem(error -> {
                                                LOG.error("Échec envoi SMS, mais Client + Prêt OK", error);
                                                return Response.ok("Client + Prêt créés, SMS échoué").build();
                                            });
                                } else {
                                    // Le prêt a échoué, on renvoie un 500
                                    return Uni.createFrom().item(
                                            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                    .entity("Échec création du Prêt")
                                                    .build()
                                    );
                                }
                            });
                })
                // Log si le flux échoue à n'importe quel moment
                .onFailure().invoke(err -> {
                    LOG.error("Erreur createClient()", err);
                });
    }

    /**
     * Envoi le SMS de bienvenue via JetfySmsService.
     */
    private Uni<Response> sendWelcomeSms(String clientUsername, String password) {
        String message = String.format(
                "Bienvenue chez Flot! Vos identifiants:\nIdentifiant: %s\nMot de passe: %s",
                clientUsername, password
        );
        return smsService.sendSMS(clientUsername, message);
    }

    /**
     * Construit le JsonObject pour la requête Fineract (création client).
     */
    private JsonObject createFineractRequest(CreateFeneratClientCommande cmd) {
        Map<String, Object> request = new HashMap<>();
        if (cmd.getFullname() != null && !cmd.getFullname().isBlank()) {
            request.put("fullname", cmd.getFullname());
        } else {
            request.put("firstname", cmd.getFirstname());
            request.put("lastname", cmd.getLastname());
            if (cmd.getMiddlename() != null) {
                request.put("middlename", cmd.getMiddlename());
            }
        }

        request.put("officeId", cmd.getOfficeId());
        request.put("active", cmd.getActive());
        request.put("locale", cmd.getLocale() != null ? cmd.getLocale() : "fr");
        request.put("dateFormat", cmd.getDateFormat() != null ? cmd.getDateFormat() : "dd MMMM yyyy");

        if (Boolean.TRUE.equals(cmd.getActive())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);
            if (cmd.getActivationDate() != null) {
                request.put("activationDate", cmd.getActivationDate());
            } else {
                request.put("activationDate", LocalDateTime.now().format(formatter));
            }
        }

        // Exemple de formatage du téléphone
        if (cmd.getMobileNo() != null) {
            request.put("mobileNo", formatPhoneNumber(cmd.getMobileNo()));
        }

        // Etc. (autres champs à ajouter)

        return JsonObject.mapFrom(request);
    }

    /**
     * Calcule un montant de prêt hypothétique basé sur le Lead (salaire, etc.).
     */
    private BigDecimal calculateLoanAmount(Lead lead) {
        BigDecimal salary = lead.getSalary() != null ? lead.getSalary() : BigDecimal.ZERO;
        BigDecimal expenses = lead.getExpenses() != null ? lead.getExpenses() : BigDecimal.ZERO;
        BigDecimal spouseIncome = lead.getSpouseIncome() != null ? lead.getSpouseIncome() : BigDecimal.ZERO;

        BigDecimal totalMonthlyIncome = salary.add(spouseIncome);
        BigDecimal repaymentCapacity = totalMonthlyIncome.subtract(expenses);
        BigDecimal maxLoanAmount = repaymentCapacity.multiply(BigDecimal.valueOf(36));

        BigDecimal minLoanAmount = BigDecimal.valueOf(1_000_000);
        BigDecimal maxProductLimit = BigDecimal.valueOf(50_000_000);

        if (maxLoanAmount.compareTo(minLoanAmount) < 0) {
            maxLoanAmount = minLoanAmount;
        } else if (maxLoanAmount.compareTo(maxProductLimit) > 0) {
            maxLoanAmount = maxProductLimit;
        }

        return maxLoanAmount
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Ex.: "22501234567" -> "01234567"
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 3) {
            return phoneNumber;
        }
        return phoneNumber.substring(3);
    }

    /**
     * Valide les champs obligatoires avant de créer un client Fineract.
     */
    private void validateCommand(CreateFeneratClientCommande cmd) {
        LOG.info("Data commande : " + cmd.getFullname());
        if ((cmd.getFullname() == null || cmd.getFullname().isBlank()) &&
                (cmd.getFirstname() == null || cmd.getLastname() == null)) {
            throw new BusinessException("Soit fullname, soit firstname/lastname doit être fourni");
        }
        if (cmd.getOfficeId() == null) {
            throw new BusinessException("officeId obligatoire");
        }
        if (Boolean.TRUE.equals(cmd.getActive()) && cmd.getActivationDate() == null) {
            throw new BusinessException("activationDate obligatoire quand active=true");
        }
    }
}
