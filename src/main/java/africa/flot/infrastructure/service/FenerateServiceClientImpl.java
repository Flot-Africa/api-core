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
import io.smallrye.mutiny.Uni;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class FenerateServiceClientImpl {
    private static final Logger LOG = Logger.getLogger(FenerateServiceClientImpl.class);

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

    private Client httpClient;
    private final ExecutorService executorService;

    public FenerateServiceClientImpl() {
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @PostConstruct
    void init() {
        httpClient = ClientBuilder.newClient();
    }

    @PreDestroy
    void cleanup() {
        if (httpClient != null) {
            httpClient.close();
        }
        executorService.shutdown();
    }

    public Uni<Response> createClient(InitLoanCommande commande) {
        return Lead.<Lead>findById(commande.getLeadId())
                .onItem().ifNull().failWith(() -> new NotFoundException("Lead not found with id: " + commande.getLeadId()))
                .flatMap(lead -> {
                    CreateFeneratClientCommande command = LeadToFeneratClientMapper.toCommand(lead);

                    return Uni.createFrom().emitter(em -> CompletableFuture.runAsync(() -> {
                        try {
                            validateCommand(command);
                            String requestBody = createFineractRequest(command);
                            Response response = sendFineractRequest(requestBody);

                            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                                JsonNode clientResponse = response.readEntity(JsonNode.class);
                                Integer clientId = clientResponse.get("clientId").asInt();

                                // Création du prêt
                                loanService.createLoan(clientId, commande.getProduitId(), calculateLoanAmount(lead))
                                        .subscribe().with(
                                                loanResponse -> {
                                                    if (loanResponse.getStatus() == Response.Status.OK.getStatusCode()) {
                                                        LOG.info("Prêt créé avec succès pour le client " + clientId);

                                                        // Envoi du SMS de bienvenue
                                                        String generatedPassword = PasswordGenerator.generate();
                                                        String clientUsername = formatPhoneNumber(command.getMobileNo());
                                                        sendWelcomeSms(clientUsername, generatedPassword)
                                                                .subscribe().with(
                                                                        smsResponse -> em.complete(response),
                                                                        error -> {
                                                                            LOG.error("Erreur lors de l'envoi du SMS mais création client et prêt OK", error);
                                                                            em.complete(response);
                                                                        }
                                                                );
                                                    } else {
                                                        LOG.error("Échec de la création du prêt: " + loanResponse.getStatus());
                                                        em.fail(new BusinessException("Échec de la création du prêt"));
                                                    }
                                                },
                                                error -> {
                                                    LOG.error("Erreur lors de la création du prêt", error);
                                                    em.fail(error);
                                                }
                                        );
                            } else {
                                em.complete(response);
                            }
                        } catch (Exception e) {
                            em.fail(e);
                        }
                    }, executorService));
                });
    }

    private BigDecimal calculateLoanAmount(Lead lead) {
        // Implémentation du calcul du montant du prêt
        BigDecimal salary = lead.getSalary() != null ? lead.getSalary() : BigDecimal.ZERO;
        BigDecimal expenses = lead.getExpenses() != null ? lead.getExpenses() : BigDecimal.ZERO;
        BigDecimal spouseIncome = lead.getSpouseIncome() != null ? lead.getSpouseIncome() : BigDecimal.ZERO;

        // Calculer le revenu mensuel total
        BigDecimal totalMonthlyIncome = salary.add(spouseIncome);

        // Calculer la capacité de remboursement (revenu - dépenses)
        BigDecimal repaymentCapacity = totalMonthlyIncome.subtract(expenses);

        // Calculer le montant maximum du prêt
        // Par exemple: capacité de remboursement × 36 mois (durée du prêt)
        BigDecimal maxLoanAmount = repaymentCapacity.multiply(BigDecimal.valueOf(36));

        // Appliquer les limites du produit
        BigDecimal minLoanAmount = BigDecimal.valueOf(1_000_000);
        BigDecimal maxProductLimit = BigDecimal.valueOf(50_000_000);

        // S'assurer que le montant est dans les limites
        if (maxLoanAmount.compareTo(minLoanAmount) < 0) {
            maxLoanAmount = minLoanAmount;
        } else if (maxLoanAmount.compareTo(maxProductLimit) > 0) {
            maxLoanAmount = maxProductLimit;
        }

        // Arrondir au multiple de 100 le plus proche
        return maxLoanAmount.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));
    }


    private void validateCommand(CreateFeneratClientCommande command) {
        List<String> errors = new ArrayList<>();

        // Validation du nom
        if (!isValidNameCombination(command)) {
            errors.add("Soit firstname/lastname, soit fullname doit être fourni");
        }

        // Validation de l'officeId
        if (command.getOfficeId() == null) {
            errors.add("L'ID de l'agence est obligatoire");
        }

        // Validation de l'activation
        if (Boolean.TRUE.equals(command.getActive()) && command.getActivationDate() == null) {
            errors.add("La date d'activation est obligatoire quand le client est actif");
        }

        // Validation de l'adresse si activée
        if (command.getAddress() != null && command.getAddress().isEmpty()) {
            errors.add("La liste d'adresses ne peut pas être vide si elle est fournie");
        }

        // Validation du numéro de téléphone pour l'envoi du SMS
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

    private String createFineractRequest(CreateFeneratClientCommande command) {
        try {
            Map<String, Object> request = new HashMap<>();

            // Gestion du nom
            if (command.getFullname() != null) {
                request.put("fullname", command.getFullname());
            } else {
                request.put("firstname", command.getFirstname());
                request.put("lastname", command.getLastname());
                if (command.getMiddlename() != null) {
                    request.put("middlename", command.getMiddlename());
                }
            }

            // Informations obligatoires
            request.put("officeId", command.getOfficeId());
            request.put("active", command.getActive());
            request.put("locale", command.getLocale() != null ? command.getLocale() : "fr");
            request.put("dateFormat", command.getDateFormat() != null ? command.getDateFormat() : "dd MMMM yyyy");

            // Gestion de la date d'activation
            if (Boolean.TRUE.equals(command.getActive())) {
                String locale = command.getLocale() != null ? command.getLocale() : "fr";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag(locale));

                if (command.getActivationDate() != null) {
                    // utilise la forme : 17 décembre 2024 pour que sa puise marchez
                    request.put("activationDate", command.getActivationDate());
                } else {
                    request.put("activationDate", LocalDateTime.now().format(formatter));
                }
            }

            // Gestion des champs optionnels
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

            // Gestion de la date de naissance
            if (command.getDateOfBirth() != null) {
                LocalDateTime dateOfBirth = command.getDateOfBirth().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(command.getDateFormat());
                request.put("dateOfBirth", dateOfBirth.format(formatter));
            }

            // Gestion des adresses
            if (command.getAddress() != null && !command.getAddress().isEmpty()) {
                request.put("address", command.getAddress());
            }

            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de la création de la requête Fineract: " + e.getMessage());
        }
    }

    private Response sendFineractRequest(String requestBody) {
        try {
            LOG.debug("Envoi de la requête à Fineract: " + requestBody);

            Response response = httpClient.target(apiUrl + "/v1/clients")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", getBasicAuthHeader())
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("Fineract-Platform-TenantId", "default")
                    .post(Entity.json(requestBody));

            LOG.info("Réponse Fineract reçue avec le status: " + response.getStatus());
            return response;
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de l'appel à l'API Fineract: " + e.getMessage());
        }
    }

    private Uni<Response> sendWelcomeSms(String clientUsername, String password) {
        String message = String.format(
                "Bienvenue chez Flot! Vos identifiants de connexion sont:\nIdentifiant: %s\nMot de passe: %s",
                clientUsername,
                password
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

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 3) {
            return phoneNumber;
        }
        return phoneNumber.substring(3);
    }
}