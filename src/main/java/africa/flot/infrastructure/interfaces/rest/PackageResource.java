package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.query.PackageDTO;
import africa.flot.application.ports.PackageService;
import africa.flot.domain.model.Account;
import africa.flot.domain.model.LeadScore;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.infrastructure.mappers.PackageMappers;
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

@Path("/packages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PackageResource {

    private static final Logger LOG = Logger.getLogger(PackageResource.class);

    @Inject
    PackageService packageService;

    // PackageResource.java
    @GET
    @Path("/available/{leadId}")
    @RolesAllowed("ADMIN")
    public Uni<Response> getAvailablePackages(@PathParam("leadId") UUID leadId) {
        return LeadScore.<LeadScore>find("leadId", leadId)
                .firstResult()
                .flatMap(Unchecked.function(score -> {
                    if (score == null) {
                        throw new NotFoundException("Score non trouvé pour le lead: " + leadId);
                    }
                    DetailedScore detailedScore = new DetailedScore(
                            score.getPersonalDataScore(),
                            score.getVtcExperienceScore(),
                            score.getDrivingRecordScore(),
                            score.getTotalScore()
                    );
                    return packageService.getPackagesForScore(detailedScore)
                            .map(ApiResponseBuilder::success);
                }))
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof NotFoundException) {
                        return ApiResponseBuilder.failure(
                                throwable.getMessage(),
                                Response.Status.NOT_FOUND
                        );
                    }
                    LOG.error("Erreur lors de la récupération des forfaits disponibles", throwable);
                    return ApiResponseBuilder.failure(
                            "Une erreur est survenue lors de la récupération des forfaits.",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }



}
