package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.command.PackageSubscriptionCommand;
import africa.flot.application.ports.AccountService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    private static final Logger LOG = Logger.getLogger(AccountResource.class);

    @Inject
    AccountService accountService;

    @POST
    @Path("/{leadId}/subscribe")
    @RolesAllowed("ADMIN")
    public Uni<Response> subscribeToPackage(@PathParam("leadId") UUID leadId, PackageSubscriptionCommand command) {
        return accountService.subscribeToPackage(leadId, command.getPackageId())
                .map(smsResponse -> {
                    if (smsResponse.getStatus() == 200) {
                        return ApiResponseBuilder.success("Compte créé et SMS envoyé avec succès");
                    } else {
                        return ApiResponseBuilder.failure(
                                "Échec de l'envoi du SMS. Statut: " + smsResponse.getStatus(),
                                Response.Status.fromStatusCode(smsResponse.getStatus())
                        );
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Erreur lors de la souscription au forfait", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR);
                });
    }
}
