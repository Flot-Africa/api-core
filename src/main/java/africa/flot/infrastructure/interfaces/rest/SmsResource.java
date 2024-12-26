package africa.flot.infrastructure.interfaces.rest;

import africa.flot.domain.model.Account;
import africa.flot.infrastructure.service.JetfySmsService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/sms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SmsResource {

    private static final Logger LOG = Logger.getLogger(SmsResource.class);

    @Inject
    JetfySmsService smsService;

    @GET
    @Path("/balance")
    @RolesAllowed("ADMIN")
    public Uni<Response> getSmsBalance() {
        return smsService.getSmsBalance()
                .map(balance -> ApiResponseBuilder.success(Map.of(
                        "balance", balance,
                        "currency", "XOF",
                        "sms_count", balance / JetfySmsService.COST_PER_SMS
                )))
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors de la récupération du solde SMS: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération du solde SMS",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @POST
    @Path("/send")
    @RolesAllowed("ADMIN")
    public Uni<Response> sendSms(@Valid SendSmsRequest request) {
        LOG.infof("Tentative d'envoi de SMS à %s", request.phoneNumber);

        // Créer un compte temporaire pour le test
        Account tempAccount = new Account();
        tempAccount.setUsername(request.phoneNumber);

        return smsService.sendSMS(request.phoneNumber, request.message, tempAccount)
                .map(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        return ApiResponseBuilder.success(Map.of(
                                "message", "SMS envoyé avec succès",
                                "recipient", request.phoneNumber,
                                "smsCount", calculateSmsCount(request.message)
                        ));
                    } else {
                        return ApiResponseBuilder.failure(
                                "Échec de l'envoi du SMS",
                                Response.Status.fromStatusCode(response.getStatus())
                        );
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors de l'envoi du SMS: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de l'envoi du SMS: " + throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    private int calculateSmsCount(String message) {
        return (message.length() + 159) / 160;
    }

    public static class SendSmsRequest {
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        public String phoneNumber;

        @NotBlank(message = "Le message est obligatoire")
        public String message;
    }
}