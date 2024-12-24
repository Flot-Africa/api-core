package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.infrastructure.service.FeneractServiceClientImpl;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;


@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {

    private static final Logger LOG = Logger.getLogger(ClientResource.class);

    @Inject
    FeneractServiceClientImpl finerateService;

    @POST
    @RolesAllowed("ADMIN")
    public Uni<Response> createClient(@RequestBody InitLoanCommande command) {
        return finerateService.createClient(command)
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Erreur lors de la creation d'un client", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR);
                });
    }
}