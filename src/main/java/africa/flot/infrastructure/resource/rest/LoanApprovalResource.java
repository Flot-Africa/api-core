package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.command.CreateLoanCommand;
import africa.flot.application.service.FlotLoanService;
import africa.flot.application.usecase.lead.CalculateLeadScore;
import africa.flot.domain.model.LeadScore;
import africa.flot.domain.model.Vehicle;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Path("/loan-approval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Approbation de Prêt", description = "APIs pour le scoring des leads et la création automatique de prêts")
public class LoanApprovalResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    private static final double APPROVAL_THRESHOLD = 7.0; // Sur 10

    @Inject
    CalculateLeadScore calculateLeadScore;

    @Inject
    FlotLoanService flotLoanService;

    @POST
    @Path("/{leadId}/score-and-create-loan")
    @RolesAllowed("ADMIN")
    @Operation(
            summary = "Scorer un lead et créer un prêt si approuvé",
            description = "Calcule le score du lead et crée automatiquement un prêt si le score atteint les critères d'approbation"
    )
    @APIResponse(
            responseCode = "201",
            description = "Prêt créé avec succès après scoring"
    )
    @APIResponse(
            responseCode = "200",
            description = "Lead scoré mais n'a pas atteint les critères d'approbation"
    )
    @APIResponse(
            responseCode = "500",
            description = "Erreur durant le scoring ou la création du prêt"
    )
    public Uni<Response> scoreAndCreateLoan(
            @Parameter(description = "UUID du lead à scorer", required = true)
            @PathParam("leadId") UUID leadId,
            @Parameter(description = "ID du véhicule à utiliser pour la création du prêt si le score est approuvé", required = true)
            @QueryParam("vehicleId") UUID vehicleId) {

        BUSINESS_LOG.infof("Évaluation du lead %s pour un prêt avec le véhicule %s", leadId, vehicleId);

        // 1. Calcul du score du lead
        return calculateLeadScore.execute(leadId)
                .flatMap(score -> {
                    BUSINESS_LOG.infof("Score calculé pour le lead %s: %.2f/10", leadId, score.getTotalScore());

                    // 2. Vérifier si le score est suffisant pour approbation
                    if (score.getTotalScore() >= APPROVAL_THRESHOLD &&
                            score.getVtcExperienceScore() > 1.5) { // 1.5/10 correspond à 15/100 dans le scoring V2

                        BUSINESS_LOG.infof("Score approuvé pour le lead %s, création d'un prêt", leadId);

                        // 3. Créer le prêt si le score est suffisant
                        return createLoanForApprovedLead(leadId, vehicleId, score);
                    } else {
                        BUSINESS_LOG.infof("Score insuffisant pour le lead %s (%.2f/10), prêt non créé",
                                leadId, score.getTotalScore());

                        return Uni.createFrom().item(
                                ApiResponseBuilder.success(
                                        Map.of(
                                                "score", score,
                                                "approved", false,
                                                "message", "Le score est insuffisant pour l'approbation du prêt"
                                        )
                                )
                        );
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Erreur lors du processus d'évaluation et de création de prêt", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @WithSession
    protected Uni<Response> createLoanForApprovedLead(UUID leadId, UUID vehicleId, DetailedScore score) {
        BUSINESS_LOG.infof("Création d'un prêt pour le lead approuvé %s avec le véhicule %s", leadId, vehicleId);

        // Vérifier si le véhicule existe
        return Vehicle.<Vehicle>findById(vehicleId)
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Véhicule introuvable: " + vehicleId))
                .flatMap(vehicle -> {
                    // Créer la commande de prêt
                    CreateLoanCommand command = new CreateLoanCommand();
                    command.setLeadId(leadId);
                    command.setVehicleId(vehicleId);
                    // Autres paramètres si nécessaire...

                    // Appeler le service pour créer le prêt
                    return flotLoanService.createLoan(command)
                            .map(loan -> {
                                AUDIT_LOG.infof("Prêt créé automatiquement après approbation - Lead: %s, Loan: %s",
                                        leadId, loan.getId());

                                Map<String, Object> response = new HashMap<>();
                                response.put("score", score);
                                response.put("approved", true);
                                response.put("loanId", loan.getId());
                                response.put("message", "Prêt créé avec succès suite à l'approbation du score");

                                return ApiResponseBuilder.success(response, Response.Status.CREATED);
                            });
                });
    }

    /**
     * Calcule le score d'un lead sans valider automatiquement
     * Cette méthode permet d'obtenir le score d'un lead sans créer de prêt
     */
    @POST
    @Path("/{leadId}/calculate-score")
    @RolesAllowed("ADMIN")
    @Operation(
            summary = "Calculer uniquement le score d'un lead",
            description = "Calcule le score du lead sans créer automatiquement un prêt, permettant une évaluation sans engagement"
    )
    @APIResponse(
            responseCode = "200",
            description = "Score calculé avec succès"
    )
    @APIResponse(
            responseCode = "500",
            description = "Erreur durant le calcul du score"
    )
    public Uni<Response> calculateScoreOnly(
            @Parameter(description = "UUID du lead à scorer", required = true)
            @PathParam("leadId") UUID leadId) {

        BUSINESS_LOG.infof("Calcul du score pour le lead %s sans validation automatique", leadId);

        return calculateLeadScore.execute(leadId)
                .map(score -> {
                    BUSINESS_LOG.infof("Score calculé pour le lead %s: %.2f/10", leadId, score.getTotalScore());

                    boolean isApproved = score.getTotalScore() >= APPROVAL_THRESHOLD &&
                            score.getVtcExperienceScore() > 1.5;

                    Map<String, Object> response = new HashMap<>();
                    response.put("score", score);
                    response.put("eligibleForApproval", isApproved);
                    response.put("message", isApproved ?
                            "Le lead est éligible pour un prêt" :
                            "Le lead n'est pas éligible pour un prêt actuellement");

                    AUDIT_LOG.infof("Score calculé sans validation automatique - Lead: %s, Score: %.2f/10, Éligible: %s",
                            leadId, score.getTotalScore(), isApproved);

                    return ApiResponseBuilder.success(response);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Erreur lors du calcul du score", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    /**
     * Approuve manuellement un lead déjà scoré
     * Cette méthode permet de créer un prêt pour un lead dont le score a déjà été calculé
     */
    @POST
    @Path("/{leadId}/approve-manually")
    @RolesAllowed("ADMIN")
    @Operation(
            summary = "Approuver manuellement un lead",
            description = "Crée un prêt pour un lead déjà scoré, en contournant les vérifications automatiques de score"
    )
    @APIResponse(
            responseCode = "201",
            description = "Prêt créé avec succès"
    )
    @APIResponse(
            responseCode = "404",
            description = "Lead ou véhicule introuvable"
    )
    @APIResponse(
            responseCode = "500",
            description = "Erreur durant la création du prêt"
    )
    @WithSession
    public Uni<Response> approveManually(
            @Parameter(description = "UUID du lead à approuver", required = true)
            @PathParam("leadId") UUID leadId,
            @Parameter(description = "ID du véhicule à utiliser pour la création du prêt", required = true)
            @QueryParam("vehicleId") UUID vehicleId) {

        BUSINESS_LOG.infof("Approbation manuelle du lead %s pour un prêt avec le véhicule %s", leadId, vehicleId);

        // Vérifier si un score existe pour ce lead
        return LeadScore.<LeadScore>find("leadId", leadId)
                .firstResult()
                .onItem().ifNull().failWith(() ->
                        new IllegalStateException("Aucun score n'existe pour ce lead. Veuillez d'abord calculer un score."))
                .flatMap(leadScore -> {
                    BUSINESS_LOG.infof("Score existant trouvé pour le lead %s: %.2f/10",
                            leadId, leadScore.getTotalScore());

                    // Convertir LeadScore en DetailedScore pour la réponse
                    DetailedScore detailedScore = new DetailedScore(
                            leadScore.getPersonalDataScore(),
                            leadScore.getDrivingRecordScore(),
                            leadScore.getVtcExperienceScore(),
                            leadScore.getTotalScore()
                    );

                    // Créer le prêt
                    return Vehicle.<Vehicle>findById(vehicleId)
                            .onItem().ifNull().failWith(() ->
                                    new IllegalArgumentException("Véhicule introuvable: " + vehicleId))
                            .flatMap(vehicle -> {
                                CreateLoanCommand command = new CreateLoanCommand();
                                command.setLeadId(leadId);
                                command.setVehicleId(vehicleId);

                                return flotLoanService.createLoan(command)
                                        .map(loan -> {
                                            AUDIT_LOG.infof("Prêt créé manuellement - Lead: %s, Loan: %s",
                                                    leadId, loan.getId());

                                            Map<String, Object> response = new HashMap<>();
                                            response.put("score", detailedScore);
                                            response.put("approved", true);
                                            response.put("manuallyApproved", true);
                                            response.put("loanId", loan.getId());
                                            response.put("message", "Prêt créé avec succès suite à l'approbation manuelle");

                                            // Mettre à jour le statut d'approbation du score si nécessaire
                                            if (!leadScore.isApproved()) {
                                                leadScore.setApproved(true);
                                                leadScore.persistAndFlush().subscribe().with(
                                                        success -> BUSINESS_LOG.info("Statut d'approbation du score mis à jour"),
                                                        error -> ERROR_LOG.error("Erreur lors de la mise à jour du score", error)
                                                );
                                            }

                                            return ApiResponseBuilder.success(response, Response.Status.CREATED);
                                        });
                            });
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Erreur lors de l'approbation manuelle", throwable);
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }
}