package africa.flot.infrastructure.service;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.application.mappers.LeadToFeneratClientMapper;
import africa.flot.domain.model.Lead;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.util.PasswordGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import io.vertx.mutiny.core.Vertx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;

@ApplicationScoped
public class FeneractServiceClientImpl {
    private static final Logger LOG = Logger.getLogger(FeneractServiceClientImpl.class);

    @ConfigProperty(name = "fineract.api.username")
    String username;

    @ConfigProperty(name = "fineract.api.password")
    String password;

    @ConfigProperty(name = "fineract.api.url")
    String apiUrl;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JetfySmsService smsService;

    @Inject
    LoanService loanService;

    // Vert.x réactif
    @Inject
    Vertx vertx;

    // On définit un Executor qui exécute la tâche sur le Context Vert.x
    // (donc sur l'event loop).
    private Executor vertxExecutor;

    private Client httpClient;

    @PostConstruct
    void init() {
        httpClient = ClientBuilder.newClient();

        // Crée un Executor à partir du Context Vert.x
        // .emitOn(...) exige un Executor, pas un Context
        vertxExecutor = command -> {
            // On utilise runOnContext(...) pour rebasculer sur l’event loop
            vertx.getOrCreateContext().runOnContext(command);
        };
    }

    @PreDestroy
    void cleanup() {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * Méthode principale pour créer un client dans Fineract, créer un prêt
     * et envoyer le SMS de bienvenue.
     */
    @WithSession
    public Uni<Response> createClient(InitLoanCommande commande) {
        // 1) Reste sur l’event loop pour Hibernate Reactive
        return Lead.<Lead>findById(commande.getLeadId())
                .onItem().ifNull().failWith(() -> new NotFoundException("Lead not found with id: " + commande.getLeadId()))
                .flatMap(lead -> {
                    try {
                        // Toujours sur l’event loop => OK pour Hibernate
                        CreateFeneratClientCommande command = LeadToFeneratClientMapper.toCommand(lead);
                        validateCommand(command);
                        String requestBody = createFineractRequest(command);

                        // 2) Envoie l’appel HTTP bloquant sur un worker pool
                        return sendFineractRequestInWorkerThread(requestBody)
                                // 3) Reviens ensuite sur l’event loop avant de relancer du Hibernate Reactive
                                .emitOn(vertxExecutor)
                                .flatMap(response -> {
                                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                                        JsonNode clientResponse = response.readEntity(JsonNode.class);
                                        Integer clientId = clientResponse.get("clientId").asInt();

                                        // Calcul du montant
                                        BigDecimal loanAmount = calculateLoanAmount(lead);

                                        return createLoanAndSendWelcomeSms(clientId, commande, command, loanAmount, response);
                                    }
                                    // En cas de status != 200, on renvoie directement la réponse
                                    return Uni.createFrom().item(response);
                                });
                    } catch (Exception e) {
                        return Uni.createFrom().failure(e);
                    }
                });
    }

    /**
     * Envoie la requête Fineract sur un thread worker (appel bloquant).
     */
    private Uni<Response> sendFineractRequestInWorkerThread(String requestBody) {
        return Uni.createFrom().item(() -> {
                    try {
                        return getResponse(requestBody);  // Appel bloquant
                    } catch (Exception e) {
                        throw new BusinessException("Erreur lors de l'appel à Fineract: " + e.getMessage());
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * Appel HTTP bloquant à Fineract (via JAX-RS Client).
     */
    @NotNull
    private Response getResponse(String requestBody) {
        try {
            LOG.debug("Envoi requête Fineract: " + requestBody);
            Response response = httpClient.target(apiUrl + "/v1/clients")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", getBasicAuthHeader())
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("Fineract-Platform-TenantId", "default")
                    .post(Entity.json(requestBody));

            LOG.info("Réponse Fineract, status: " + response.getStatus());
            return response;
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de l'appel à l'API Fineract: " + e.getMessage());
        }
    }

    /**
     * Après la création du client, on crée le prêt et on envoie un SMS.
     * Ici, on est revenu sur l’event loop => on peut appeler du Hibernate Reactive.
     */
    private Uni<Response> createLoanAndSendWelcomeSms(Integer clientId,
                                                      InitLoanCommande commande,
                                                      CreateFeneratClientCommande command,
                                                      BigDecimal loanAmount,
                                                      Response originalResponse) {
        return loanService.createLoan(clientId, commande.getProduitId(), loanAmount)
                .flatMap(loanResponse -> {
                    if (loanResponse.getStatus() == Response.Status.OK.getStatusCode()) {
                        LOG.info("Prêt créé avec succès pour client " + clientId);

                        String generatedPassword = PasswordGenerator.generate();
                        String clientUsername = formatPhoneNumber(command.getMobileNo());

                        return sendWelcomeSms(clientUsername, generatedPassword)
                                .map(smsResponse -> originalResponse)
                                .onFailure().recoverWithItem(error -> {
                                    LOG.error("Erreur lors de l'envoi du SMS (client/prêt OK)", error);
                                    return originalResponse;
                                });
                    }
                    LOG.error("Échec création du prêt: " + loanResponse.getStatus());
                    return Uni.createFrom().failure(new BusinessException("Échec de la création du prêt"));
                });
    }

    /**
     * Calcule un montant de prêt "maximum" sur base salaire, dépenses, etc.
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
     * Vérifie la validité du DTO CreateFeneratClientCommande.
     */
    private void validateCommand(CreateFeneratClientCommande command) {
        List<String> errors = new ArrayList<>();

        if (!isValidNameCombination(command)) {
            errors.add("Soit firstname/lastname, soit fullname doit être fourni");
        }

        if (command.getOfficeId() == null) {
            errors.add("L'ID de l'agence est obligatoire");
        }

        if (Boolean.TRUE.equals(command.getActive()) && command.getActivationDate() == null) {
            errors.add("La date d'activation est obligatoire quand le client est actif");
        }

        if (command.getAddress() != null && command.getAddress().isEmpty()) {
            errors.add("La liste d'adresses ne peut pas être vide si elle est fournie");
        }

        if (command.getMobileNo() == null || command.getMobileNo().trim().isEmpty()) {
            errors.add("Le numéro de téléphone est obligatoire pour l'envoi des identifiants");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException("Erreurs de validation: " + String.join(", ", errors));
        }
    }

    private boolean isValidNameCombination(CreateFeneratClientCommande command) {
        boolean hasPersonName = command.getFirstname() != null && command.getLastname() != null;
        boolean hasFullName = command.getFullname() != null && !command.getFullname().trim().isEmpty();
        return hasPersonName || hasFullName;
    }

    /**
     * Construit le JSON d'appel pour créer un client dans Fineract.
     */
    private String createFineractRequest(CreateFeneratClientCommande command) {
        try {
            Map<String, Object> request = new HashMap<>();

            if (command.getFullname() != null) {
                request.put("fullname", command.getFullname());
            } else {
                request.put("firstname", command.getFirstname());
                request.put("lastname", command.getLastname());
                if (command.getMiddlename() != null) {
                    request.put("middlename", command.getMiddlename());
                }
            }

            request.put("officeId", command.getOfficeId());
            request.put("active", command.getActive());
            request.put("locale", command.getLocale() != null ? command.getLocale() : "fr");
            request.put("dateFormat", command.getDateFormat() != null ? command.getDateFormat() : "dd MMMM yyyy");

            if (Boolean.TRUE.equals(command.getActive())) {
                String locale = command.getLocale() != null ? command.getLocale() : "fr";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag(locale));

                if (command.getActivationDate() != null) {
                    request.put("activationDate", command.getActivationDate());
                } else {
                    // Si non fourni, on prend la date courante
                    request.put("activationDate", LocalDateTime.now().format(formatter));
                }
            }

            addIfNotNull(request, "groupId", command.getGroupId());
            addIfNotNull(request, "externalId", command.getExternalId());
            addIfNotNull(request, "accountNo", command.getAccountNo());
            addIfNotNull(request, "staffId", command.getStaffId());
            addIfNotNull(request, "mobileNo", formatPhoneNumber(command.getMobileNo()));
            addIfNotNull(request, "savingsProductId", command.getSavingsProductId());
            addIfNotNull(request, "genderId", command.getGenderId());
            addIfNotNull(request, "clientTypeId", command.getClientTypeId());
            addIfNotNull(request, "clientClassificationId", command.getClientClassificationId());
            addIfNotNull(request, "legalFormId", command.getLegalFormId());
            addIfNotNull(request, "emailAddress", command.getEmailAddress());

            if (command.getDateOfBirth() != null) {
                LocalDateTime dateOfBirth = command.getDateOfBirth().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.getDateFormat());
                request.put("dateOfBirth", dateOfBirth.format(formatter));
            }

            if (command.getAddress() != null && !command.getAddress().isEmpty()) {
                request.put("address", command.getAddress());
            }

            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de la création de la requête Fineract: " + e.getMessage());
        }
    }

    /**
     * Envoi du SMS de bienvenue.
     */
    private Uni<Response> sendWelcomeSms(String clientUsername, String password) {
        String message = String.format(
                "Bienvenue chez Flot! Vos identifiants de connexion sont:\nIdentifiant: %s\nMot de passe: %s",
                clientUsername, password
        );
        return smsService.sendSMS(clientUsername, message);
    }

    private void addIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private String getBasicAuthHeader() {
        if (username == null || password == null) {
            throw new IllegalStateException("Les credentials Fineract ne sont pas configurés");
        }
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Par ex. "22501234567" -> "01234567" (selon votre logique).
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 3) {
            return phoneNumber;
        }
        // Retire par exemple l'indicatif
        return phoneNumber.substring(3);
    }
}
