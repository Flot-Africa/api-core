// 1. FenerateServiceClientImpl.java
package africa.flot.infrastructure.service;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.domain.model.exception.BusinessException;
import africa.flot.infrastructure.util.PasswordGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    public Uni<Response> createClient(CreateFeneratClientCommande command) {
        return Uni.createFrom().emitter(em -> {
            CompletableFuture.runAsync(() -> {
                try {
                    validateCommand(command);
                    String requestBody = createFineractRequest(command);
                    Response response = sendFineractRequest(requestBody);

                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        String generatedPassword = PasswordGenerator.generate();
                        String clientUsername = formatPhoneNumber(command.getMobileNo());
                        sendWelcomeSms(clientUsername, generatedPassword)
                                .subscribe().with(
                                        smsResponse -> em.complete(response),
                                        error -> {
                                            LOG.error("Erreur SMS mais création client OK", error);
                                            em.complete(response);
                                        }
                                );
                    } else {
                        em.complete(response);
                    }
                } catch (Exception e) {
                    em.fail(e);
                }
            }, executorService);
        });
    }

    private void validateCommand(CreateFeneratClientCommande command) {
        if (command.getMobileNo() == null || command.getMobileNo().trim().isEmpty()) {
            throw new BusinessException("Le numéro de téléphone est obligatoire");
        }
        if (command.getOfficeId() == null) {
            throw new BusinessException("L'ID de l'agence est obligatoire");
        }
        if (!isValidName(command)) {
            throw new BusinessException("Le nom et prénom ou le nom complet sont obligatoires");
        }
    }

    private boolean isValidName(CreateFeneratClientCommande command) {
        return (command.getFirstname() != null && command.getLastname() != null) ||
                (command.getFullname() != null && !command.getFullname().trim().isEmpty());
    }

    private String createFineractRequest(CreateFeneratClientCommande command) {
        try {
            Map<String, Object> request = new HashMap<>();

            // Informations d'identité
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

            if (Boolean.TRUE.equals(command.getActive())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        Locale.forLanguageTag(command.getLocale() != null ? command.getLocale() : "fr"));
                request.put("activationDate", command.getActivationDate() != null ?
                        String.format(String.valueOf(formatter)) :
                        LocalDateTime.now().format(formatter));
            }

            // Informations additionnelles
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
                request.put("dateOfBirth", command.getDateOfBirth().toString());
            }

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
