package africa.flot.infrastructure.interfaces.rest;

import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Loans", description = "APIs for managing loans for mobile app and back office")
public class LoanResource {

    @Inject
    LoanService loanService;

    /**
     * Endpoint pour récupérer les détails simplifiés pour l'application mobile.
     */
    @GET
    @Path("/mobile/{externalId}")
    @Operation(summary = "Get loan details for mobile", description = "Retrieve simplified loan details for mobile users.")
    @APIResponse(
            responseCode = "200",
            description = "Simplified loan details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonObject.class))
    )
    @APIResponse(responseCode = "404", description = "Loan not found")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    public Uni<Response> getLoanDetailsForMobile(@PathParam("externalId") String externalId) {
        return loanService.getLoanDetailsForMobile(externalId)
                .map(ApiResponseBuilder::success)
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> ApiResponseBuilder.failure(
                        throwable.getMessage(),
                        Response.Status.NOT_FOUND
                ))
                .onFailure().recoverWithItem(throwable -> ApiResponseBuilder.failure(
                        "Une erreur inattendue s'est produite : " + throwable.getMessage(),
                        Response.Status.INTERNAL_SERVER_ERROR
                ));
    }

    /**
     * Endpoint pour récupérer les détails avancés pour le back-office.
     */
    @GET
    @Path("/backoffice/{externalId}")
    @RolesAllowed({"ADMIN"})
    @Operation(summary = "Get loan details for back office", description = "Retrieve detailed loan information for back office users.")
    @APIResponse(
            responseCode = "200",
            description = "Detailed loan information",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonObject.class))
    )
    @APIResponse(responseCode = "404", description = "Loan not found")
    public Uni<Response> getLoanDetailsForBackOffice(@PathParam("externalId") String externalId) {
        return loanService.getLoanDetailsForBackOffice(externalId)
                .map(ApiResponseBuilder::success)
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> ApiResponseBuilder.failure(
                        throwable.getMessage(),
                        Response.Status.NOT_FOUND
                ))
                .onFailure().recoverWithItem(throwable -> ApiResponseBuilder.failure(
                        "Une erreur inattendue s'est produite : " + throwable.getMessage(),
                        Response.Status.INTERNAL_SERVER_ERROR
                ));
    }

    /**
     * Endpoint pour récupérer l'historique des paiements d'un prêt.
     */
    @GET
    @Path("/{externalId}/repayments")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Operation(summary = "Get loan repayment history", description = "Retrieve repayment history for a given loan.")
    @APIResponse(
            responseCode = "200",
            description = "List of repayment periods",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = JsonObject.class))
    )
    @APIResponse(responseCode = "404", description = "Loan not found")
    public Uni<Response> getLoanRepaymentHistory(@PathParam("externalId") String externalId) {
        return loanService.getLoanRepaymentHistory(externalId)
                .map(ApiResponseBuilder::success)
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> ApiResponseBuilder.failure(
                        throwable.getMessage(),
                        Response.Status.NOT_FOUND
                ))
                .onFailure().recoverWithItem(throwable -> ApiResponseBuilder.failure(
                        "Une erreur inattendue s'est produite : " + throwable.getMessage(),
                        Response.Status.INTERNAL_SERVER_ERROR
                ));
    }
}
