package africa.flot.infrastructure.interfaces.rest;

import africa.flot.infrastructure.dayana.DanayaService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/documents")
public class DocumentResource {

    @Inject
    DanayaService danayaService;

    @POST
    @Path("/verify")
    public Uni<Response> verifyDocuments(DocumentRequest request) {
        return danayaService.verifyIdDocumentWithPolling(
                        request.getBucketName(),
                        request.getFrontImageName(),
                        request.getBackImageName()
                )
                .onItem().transform(result -> Response.ok(result).build())
                .onFailure().recoverWithItem(throwable ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(new ErrorResponse(throwable.getMessage()))
                                .build()
                );
    }
}

class ErrorResponse {
    private String error;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
