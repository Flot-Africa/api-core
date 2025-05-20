package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.query.RepaymentRequest;
import africa.flot.application.service.FlotLoanService;
import africa.flot.application.service.Hub2PaymentService;
import africa.flot.infrastructure.security.SecurityService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/test-v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Flot Loans", description = "APIs pour la gestion des prêts Flot")
public class Hub2SRessourceTest {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    Hub2PaymentService hub2PaymentService;

    @POST
    @Path("/payments")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
   // @PermitAll
    @Operation(summary = "Enregistrer un paiement", description = "Traite un paiement pour un prêt donné")
    @APIResponse(responseCode = "200", description = "Paiement traité avec succès")
    @APIResponse(responseCode = "404", description = "Prêt introuvable")
    @APIResponse(responseCode = "500", description = "Erreur lors du traitement du paiement")
    public Uni<Response> processPaymentTest(
            @Valid RepaymentRequest request
    ) {
        return Uni.createFrom().item(() -> {
            try {
                hub2PaymentService.repayLoanTest(request);
                AUDIT_LOG.infof("Paiement effectué avec succès pour le prêt %s");
                return Response.ok(ApiResponseBuilder.success("Paiement effectué avec succès")).build();
            } catch (Exception e) {
                ERROR_LOG.errorf(e, "Erreur lors du traitement du paiement pour le prêt %s");
                return Response.serverError().entity(ApiResponseBuilder.failure("Erreur lors du traitement du paiement", Response.Status.BAD_REQUEST)).build();
            }
        });
    }
}
