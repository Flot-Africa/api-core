package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.command.InitLoanCommande;
import africa.flot.infrastructure.service.FeneractServiceClientImpl;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Client Management", description = "API for managing clients in the system")
@Deprecated(since = "1.1.0", forRemoval = true)
public class ClientResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    FeneractServiceClientImpl fineractService;

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a client", description = "Creates a new client in the system.")
    @APIResponse(
            responseCode = "201",
            description = "Client created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Error occurred while creating the client",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> createClient(
            @RequestBody(description = "Details of the client to be created", required = true) InitLoanCommande command) {
        BUSINESS_LOG.info("Starting client creation process");
        return fineractService.createClient(command)
                .onItem().transform(clientResponse -> {
                    AUDIT_LOG.info("Client created successfully: " + command.getLeadId());
                    return ApiResponseBuilder.success(clientResponse, Response.Status.CREATED);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Error occurred while creating a client", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR);
                });
    }
}
