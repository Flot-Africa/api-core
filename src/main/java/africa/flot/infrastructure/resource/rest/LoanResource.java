package africa.flot.infrastructure.resource.rest;

import africa.flot.domain.service.LoanService;
import africa.flot.infrastructure.security.SecurityService;
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
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Loans", description = "APIs for managing loans for mobile app and back office")
@Deprecated(since = "1.0", forRemoval = true)
public class LoanResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    LoanService loanService;

    @Inject
    SecurityService securityService;

    @GET
    @Path("/mobile/{leadId}")
    @Operation(summary = "Get loan details for mobile", description = "Retrieve simplified loan details for mobile users.")
    @APIResponse(
            responseCode = "200",
            description = "Simplified loan details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonObject.class))
    )
    @APIResponse(responseCode = "404", description = "Loan not found")
    @APIResponse(responseCode = "500", description = "Unexpected error")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    public Uni<Response> getLoanDetailsForMobile(@PathParam("leadId") UUID externalId) {
        BUSINESS_LOG.info("Fetching loan details for mobile user. Loan ID: " + externalId);

        return securityService.validateLeadAccess(externalId.toString())
                .chain(() -> loanService.getLoanDetailsForMobile(externalId))
                .map(ApiResponseBuilder::success)
                .onItem().invoke(() ->
                        AUDIT_LOG.info("Loan details retrieved successfully for mobile user. Loan ID: " + externalId))
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> {
                    ERROR_LOG.warn("Loan not found for mobile user. Loan ID: " + externalId);
                    return ApiResponseBuilder.failure("Loan not found", Response.Status.NOT_FOUND);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Unexpected error while fetching loan details for mobile user. Loan ID: " + externalId, throwable);
                    return ApiResponseBuilder.failure("Unable to retrieve loan details", Response.Status.BAD_REQUEST);
                });
    }

    @GET
    @Path("/backoffice/{leadId}")
    @RolesAllowed({"ADMIN"})
    @Operation(summary = "Get loan details for back office", description = "Retrieve detailed loan information for back office users.")
    @APIResponse(
            responseCode = "200",
            description = "Detailed loan information",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonObject.class))
    )
    @APIResponse(responseCode = "404", description = "Loan not found")
    @APIResponse(responseCode = "500", description = "Unexpected error")
    public Uni<Response> getLoanDetailsForBackOffice(@PathParam("leadId") UUID externalId) {
        BUSINESS_LOG.info("Fetching loan details for back office. Loan ID: " + externalId);

        return loanService.getLoanDetailsForBackOffice(externalId)
                .map(ApiResponseBuilder::success)
                .onItem().invoke(() ->
                        AUDIT_LOG.info("Loan details retrieved successfully for back office. Loan ID: " + externalId))
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> {
                    ERROR_LOG.warn("Loan not found for back office. Loan ID: " + externalId);
                    return ApiResponseBuilder.failure("Loan not found", Response.Status.NOT_FOUND);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Unexpected error while fetching loan details for back office. Loan ID: " + externalId, throwable);
                    return ApiResponseBuilder.failure("Unable to retrieve loan details", Response.Status.BAD_REQUEST);
                });
    }

    @GET
    @Path("/{leadId}/repayments")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    @Operation(summary = "Get loan repayment history", description = "Retrieve repayment history for a given loan.")
    @APIResponse(
            responseCode = "200",
            description = "List of repayment periods",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = JsonObject.class))
    )
    @APIResponse(responseCode = "404", description = "Loan not found")
    @APIResponse(responseCode = "500", description = "Unexpected error")
    public Uni<Response> getLoanRepaymentHistory(@PathParam("leadId") UUID externalId) {
        BUSINESS_LOG.info("Fetching repayment history for loan. Loan ID: " + externalId);

        return securityService.validateLeadAccess(externalId.toString())
                .chain(() -> loanService.getLoanRepaymentHistory(externalId))
                .map(ApiResponseBuilder::success)
                .onItem().invoke(() ->
                        AUDIT_LOG.info("Repayment history retrieved successfully. Loan ID: " + externalId))
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> {
                    ERROR_LOG.warn("Loan not found while fetching repayment history. Loan ID: " + externalId);
                    return ApiResponseBuilder.failure("Loan not found", Response.Status.NOT_FOUND);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Unexpected error while fetching repayment history. Loan ID: " + externalId, throwable);
                    return ApiResponseBuilder.failure("Unable to retrieve repayment history", Response.Status.BAD_REQUEST);
                });
    }
}