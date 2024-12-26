package africa.flot.infrastructure.service;

import africa.flot.application.ports.SmsService;
import africa.flot.domain.model.Account;
import africa.flot.infrastructure.client.JetfyClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JetfySmsService implements SmsService {

    private static final Logger LOG = Logger.getLogger(JetfySmsService.class);
    public static final int COST_PER_SMS = 15; // Coût en XOF par SMS

    @ConfigProperty(name = "quarkus.jetfy.api.token")
    String apiToken;

    @ConfigProperty(name = "quarkus.jetfy.api.sender-id")
    String senderId;

    @Inject
    @RestClient
    JetfyClient jetfyClient;

    private String getAuthHeader() {
        return "Bearer " + apiToken;
    }

    private Uni<Boolean> checkBalance(int requiredSmsCount) {
        int requiredAmount = requiredSmsCount * COST_PER_SMS;

        return jetfyClient.getBalance(getAuthHeader())
                .map(Unchecked.function(response -> {
                    if ("success".equals(response.status)) {
                        boolean hasSufficientBalance = response.data >= requiredAmount;  // Utilisation de data au lieu de balance
                        if (!hasSufficientBalance) {
                            LOG.warnf("Solde insuffisant. Requis: %d XOF (pour %d SMS), Disponible: %d XOF",
                                    requiredAmount, requiredSmsCount, response.data);
                        }
                        return hasSufficientBalance;
                    }
                    throw new RuntimeException("Échec de la récupération du solde SMS: " + response.status);
                }));
    }

    public Uni<Integer> getSmsBalance() {
        return jetfyClient.getBalance(getAuthHeader())
                .map(Unchecked.function(response -> {
                    if ("success".equals(response.status)) {
                        LOG.infof("Solde SMS actuel: %d XOF", response.data);  // Utilisation de data
                        return response.data;  // Retourne data au lieu de balance
                    }
                    LOG.errorf("Échec de la récupération du solde SMS: %s", response.status);
                    throw new RuntimeException("Échec de la récupération du solde SMS: " + response.status);
                }))
                .onFailure().invoke(error ->
                        LOG.error("Erreur lors de la récupération du solde SMS", error)
                );
    }


    @Override
    public Uni<Response> sendSMS(String phoneNumber, String message, Account account) {
        return Uni.createFrom().item(() -> {
            int smsCount = calculateSmsCount(message);
            int totalCost = smsCount * COST_PER_SMS;

            return checkBalance(smsCount)
                    .flatMap(balanceOk -> {
                        if (!balanceOk) {
                            return Uni.createFrom().item(Response.status(Response.Status.PAYMENT_REQUIRED)
                                    .entity(new ErrorResponse("error",
                                            String.format("Solde SMS insuffisant. Coût nécessaire: %d XOF pour %d SMS",
                                                    totalCost, smsCount)))
                                    .build());
                        }

                        SmsRequest smsRequest = new SmsRequest(
                                senderId,
                                phoneNumber,
                                message
                        );

                        return jetfyClient.sendSms(getAuthHeader(), smsRequest)
                                .onItem().transform(response -> {
                                    if ("success".equals(response.status)) {
                                        LOG.infof("SMS envoyé avec succès à %s. Coût: %d XOF (%d SMS)",
                                                phoneNumber, totalCost, smsCount);
                                        return Response.ok()
                                                .entity(response)
                                                .build();
                                    } else {
                                        LOG.errorf("Échec de l'envoi du SMS: %s", response.message);
                                        return Response.status(Response.Status.BAD_REQUEST)
                                                .entity(response)
                                                .build();
                                    }
                                })
                                .onFailure().recoverWithItem(error -> {
                                    LOG.error("Erreur lors de l'envoi du SMS", error);
                                    return Response.serverError()
                                            .entity(new ErrorResponse("error", error.getMessage()))
                                            .build();
                                });
                    });
        }).flatMap(uni -> uni);
    }

    private int calculateSmsCount(String message) {
        return (message.length() + 159) / 160;
    }

    public static class SmsRequest {
        @JsonProperty("sender_id")
        public String senderId;
        public String recipient;
        public String message;

        public SmsRequest(String senderId, String recipient, String message) {
            this.senderId = senderId;
            this.recipient = recipient;
            this.message = message;
        }
    }

    public static class ApiResponse {
        @JsonProperty("status")
        public String status;
        @JsonProperty("message")
        public String message;
    }

    public static class BalanceResponse {
        @JsonProperty("status")
        public String status;
        @JsonProperty("data")
        public int data;  // Le solde est dans le champ data
    }

    public static class ErrorResponse {
        @JsonProperty("status")
        public String status;
        @JsonProperty("message")
        public String message;

        public ErrorResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}