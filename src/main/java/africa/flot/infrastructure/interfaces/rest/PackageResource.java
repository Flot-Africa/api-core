package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.ports.PackageService;
import africa.flot.domain.model.Account;
import africa.flot.domain.model.LeadScore;
import africa.flot.domain.model.valueobject.DetailedScore;
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
                    // Créer un objet DetailedScore basé sur les données de LeadScore
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

    @GET
    @Path("/{leadId}/current")
    @RolesAllowed("ADMIN")
    public Uni<Response> getClientPackage(@PathParam("leadId") UUID leadId) {
        return Account.<Account>find("lead.id", leadId)
                .firstResult()
                .flatMap(Unchecked.function(account -> {
                    if (account == null || account.getSubscribedPackage() == null) {
                        throw new NotFoundException("Aucun package trouvé pour le client avec le lead: " + leadId);
                    }
                    return Uni.createFrom().item(ApiResponseBuilder.success(account.getSubscribedPackage()));
                }))
                .onFailure().recoverWithItem(throwable -> {
                    if (throwable instanceof NotFoundException) {
                        return ApiResponseBuilder.failure(
                                throwable.getMessage(),
                                Response.Status.NOT_FOUND
                        );
                    }
                    LOG.error("Erreur lors de la récupération du package du client", throwable);
                    return ApiResponseBuilder.failure(
                            "Une erreur est survenue lors de la récupération du package du client.",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }
}
