package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.command.DocumentRequest;
import africa.flot.infrastructure.repository.LeadRepository;
import africa.flot.infrastructure.service.dayana.DanayaService;
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

import java.util.UUID;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Document Management", description = "APIs for document verification and KYB status retrieval")
public class DocumentResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    DanayaService danayaService;

    @Inject
    LeadRepository leadRepository;

    @POST
    @Path("/verify")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Verify documents", description = "Verifies a lead's documents using the Danaya service.")
    @APIResponse(
            responseCode = "200",
            description = "Verification successful",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input or verification failure",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "404",
            description = "Lead not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> verifyDocuments(
            @RequestBody(description = "Details of the document to be verified", required = true) DocumentRequest request) {
        BUSINESS_LOG.info("Starting document verification for lead: " + request.getLeadId());
        return leadRepository.existsById(request.getLeadId())
                .flatMap(exists -> exists ?
                        danayaService.verifyIdDocumentWithPolling(
                                        request.getBucketName(),
                                        request.getFrontImageName(),
                                        request.getBackImageName(),
                                        request.getLeadId()
                                )
                                .onItem().invoke(result -> AUDIT_LOG.info("Document verification succeeded for lead: " + request.getLeadId()))
                                .onItem().transform(ApiResponseBuilder::success)
                                .onFailure().recoverWithItem(throwable -> {
                                    ERROR_LOG.error("Document verification failed for lead: " + request.getLeadId(), throwable);
                                    return ApiResponseBuilder.failure(throwable.getMessage(), Response.Status.BAD_REQUEST);
                                }) :
                        Uni.createFrom().item(ApiResponseBuilder.failure("Lead not found", Response.Status.NOT_FOUND))
                );
    }

    @GET
    @Path("/kyb/status/{leadId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get KYB status", description = "Fetches the Know Your Business (KYB) status of a lead.")
    @APIResponse(
            responseCode = "200",
            description = "KYB status retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "404",
            description = "Lead not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error during status retrieval",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> getKYBStatus(
            @PathParam("leadId") @Schema(description = "UUID of the lead", required = true) UUID leadId) {
        BUSINESS_LOG.info("Fetching KYB status for lead: " + leadId);
        return leadRepository.existsById(leadId)
                .flatMap(exists -> exists ?
                        danayaService.getKYBStatus(leadId)
                                .onItem().invoke(status -> AUDIT_LOG.info("KYB status retrieved for lead: " + leadId))
                                .onItem().transform(ApiResponseBuilder::success)
                                .onFailure().recoverWithItem(throwable -> {
                                    ERROR_LOG.error("Error fetching KYB status for lead: " + leadId, throwable);
                                    return ApiResponseBuilder.failure(throwable.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                                }) :
                        Uni.createFrom().item(ApiResponseBuilder.failure("Lead not found", Response.Status.NOT_FOUND))
                );
    }
}
