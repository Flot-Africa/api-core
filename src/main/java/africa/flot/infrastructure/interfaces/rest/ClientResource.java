package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.command.CreateFeneratClientCommande;
import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.infrastructure.service.FenerateServiceClientImpl;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;


// 2. ClientResource.java
@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {

    @Inject
    FenerateServiceClientImpl finerateService;

    @POST
    @Transactional
    @RolesAllowed("ADMIN")
    public Uni<Response> createClient(@RequestBody InitLoanCommande command) {
        return Uni.createFrom().item(command)
                .runSubscriptionOn(io.quarkus.runtime.ExecutorRecorder.getCurrent())
                .flatMap(cmd -> finerateService.createClient(cmd))
                .onFailure().transform(throwable ->
                        new WebApplicationException(
                                "Erreur lors de la création du client: " + throwable.getMessage(),
                                Response.Status.INTERNAL_SERVER_ERROR
                        )
                );
    }

}