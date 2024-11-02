package africa.flot.infrastructure.interfaces.rest;

import africa.flot.application.dto.command.DocumentRequest;
import africa.flot.infrastructure.repository.LeadRepository;
import africa.flot.infrastructure.service.dayana.DanayaService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/documents")
public class DocumentResource {

    @Inject
    DanayaService danayaService;

    @Inject
    LeadRepository leadRepository;

    @POST
    @Path("/verify")
    @RolesAllowed("ADMIN")
    public Uni<Response> verifyDocuments(DocumentRequest request) {
        return leadRepository.existsById(request.getLeadId())
                .flatMap(exists -> exists ?
                        danayaService.verifyIdDocumentWithPolling(
                                        request.getBucketName(),
                                        request.getFrontImageName(),
                                        request.getBackImageName(),
                                        request.getLeadId()
                                )
                                .onItem().transform(ApiResponseBuilder::success)
                                .onFailure().recoverWithItem(throwable ->
                                        ApiResponseBuilder.failure(throwable.getMessage(), Response.Status.BAD_REQUEST)
                                ) :
                        Uni.createFrom().item(ApiResponseBuilder.failure("Lead not found", Response.Status.NOT_FOUND))
                );
    }

    @GET
    @Path("/kyb/status/{leadId}")
    @RolesAllowed("ADMIN")
    public Uni<Response> getKYBStatus(@PathParam("leadId") UUID leadId) {
        return leadRepository.existsById(leadId)
                .flatMap(exists -> exists ?
                        danayaService.getKYBStatus(leadId)
                                .onItem().transform(ApiResponseBuilder::success)
                                .onFailure().recoverWithItem(throwable ->
                                        ApiResponseBuilder.failure(throwable.getMessage(), Response.Status.INTERNAL_SERVER_ERROR)
                                ) :
                        Uni.createFrom().item(ApiResponseBuilder.failure("Lead not found", Response.Status.NOT_FOUND))
                );
    }
}