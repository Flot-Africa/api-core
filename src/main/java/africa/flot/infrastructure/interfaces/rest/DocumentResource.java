package africa.flot.infrastructure.interfaces.rest;

import africa.flot.infrastructure.dayana.DanayaService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import lombok.Getter;

import java.util.UUID;

@Path("/documents")
public class DocumentResource {

    @Inject
    DanayaService danayaService;

    @POST
    @Path("/verify")
    @RolesAllowed("ADMIN")
    public Uni<Response> verifyDocuments(DocumentRequest request) {
        return danayaService.verifyIdDocumentWithPolling(
                        request.getBucketName(),
                        request.getFrontImageName(),
                        request.getBackImageName(),
                        request.getLeadId()
                )
                .onItem().transform(result -> Response.ok(result).build())
                .onFailure().recoverWithItem(throwable ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(new ErrorResponse(throwable.getMessage()))
                                .build()
                );
    }

    @GET
    @Path("/kyb/status/{leadId}")
    @RolesAllowed("ADMIN")
    public Uni<Response> getKYBStatus(@PathParam("leadId") UUID leadId) {
        return danayaService.getKYBStatus(leadId)
                .onItem().transform(status -> Response.ok(status).build())
                .onFailure().recoverWithItem(throwable ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse(throwable.getMessage()))
                                .build()
                );
    }
}

@Getter
class ErrorResponse {
    private String error;

    public ErrorResponse(String error) {
        this.error = error;
    }

}