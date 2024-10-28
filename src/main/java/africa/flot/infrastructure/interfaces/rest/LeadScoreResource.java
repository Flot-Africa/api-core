package africa.flot.infrastructure.interfaces.rest;

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
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/lead-scores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LeadScoreResource {

    private static final Logger LOG = Logger.getLogger(LeadScoreResource.class);

    @Inject
    CalculateLeadScore calculateLeadScore;

    @POST
    @Path("/{leadId}/calculate")
    @RolesAllowed("ADMIN")
    public Uni<Response> calculateLeadScore(@PathParam("leadId") UUID leadId) {
        return calculateLeadScore.execute(leadId)
                .map(ApiResponseBuilder::success)
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Erreur lors du calcul du score", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @Path("/{leadId}")
    @RolesAllowed("ADMIN")
    public Uni<Response> getLeadScore(@PathParam("leadId") UUID leadId) {
        return LeadScore.<LeadScore>find("leadId", leadId)
                .firstResult()
                .map(Unchecked.function(score -> {
                    if (score == null) {
                        throw new NotFoundException("Score non trouvé pour le lead: " + leadId);
                    }
                    return ApiResponseBuilder.success(score);
                }))
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof NotFoundException) {
                        return ApiResponseBuilder.failure(
                                throwable.getMessage(),
                                Response.Status.NOT_FOUND
                        );
                    }
                    LOG.error("Erreur lors de la récupération du score", throwable);
                    return ApiResponseBuilder.failure(
                            "Une erreur est survenue",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }
}