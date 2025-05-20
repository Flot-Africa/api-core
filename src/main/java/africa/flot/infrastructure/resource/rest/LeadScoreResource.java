package africa.flot.infrastructure.resource.rest;

import africa.flot.application.usecase.lead.CalculateLeadScore;
import africa.flot.domain.model.LeadScore;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/lead-scores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Lead Scoring", description = "APIs for calculating and retrieving lead scores")
public class LeadScoreResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    CalculateLeadScore calculateLeadScore;

    @Deprecated(since = "1.2.0", forRemoval = true)
    @POST
    @Path("/{leadId}/calculate")
    @RolesAllowed("ADMIN")
    @Operation(
            summary = "Calculate lead score",
            description = "Calculates the score for a given lead using the provided lead ID."
    )
    @APIResponse(
            responseCode = "200",
            description = "Lead score calculated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Error during lead score calculation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> calculateLeadScore(
            @Parameter(description = "UUID of the lead to calculate score for", required = true)
            @PathParam("leadId") UUID leadId) {
        BUSINESS_LOG.info("Calculating lead score for lead ID: " + leadId);
        return calculateLeadScore.execute(leadId)
                .map(ApiResponseBuilder::success)
                .onItem().invoke(() -> AUDIT_LOG.info("Lead score calculated successfully for lead ID: " + leadId))
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Error during lead score calculation for lead ID: " + leadId, throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @Path("/{leadId}")
    @RolesAllowed("ADMIN")
    @Operation(
            summary = "Retrieve lead score",
            description = "Fetches the lead score for a given lead ID."
    )
    @APIResponse(
            responseCode = "200",
            description = "Lead score retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "404",
            description = "Lead score not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Error during lead score retrieval",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> getLeadScore(
            @Parameter(description = "UUID of the lead to fetch the score for", required = true)
            @PathParam("leadId") UUID leadId) {
        BUSINESS_LOG.info("Fetching lead score for lead ID: " + leadId);
        return LeadScore.<LeadScore>find("leadId", leadId)
                .firstResult()
                .map(Unchecked.function(score -> {
                    if (score == null) {
                        ERROR_LOG.warn("Lead score not found for lead ID: " + leadId);
                        throw new NotFoundException("Score not found for lead: " + leadId);
                    }
                    AUDIT_LOG.info("Lead score retrieved successfully for lead ID: " + leadId);
                    return ApiResponseBuilder.success(score);
                }))
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof NotFoundException) {
                        return ApiResponseBuilder.failure(
                                throwable.getMessage(),
                                Response.Status.NOT_FOUND
                        );
                    }
                    ERROR_LOG.error("Error during lead score retrieval for lead ID: " + leadId, throwable);
                    return ApiResponseBuilder.failure(
                            "An error occurred",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }
}
