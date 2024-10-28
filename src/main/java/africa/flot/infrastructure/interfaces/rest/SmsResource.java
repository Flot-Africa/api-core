package africa.flot.infrastructure.interfaces.rest;

import africa.flot.infrastructure.service.JetfySmsService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
}