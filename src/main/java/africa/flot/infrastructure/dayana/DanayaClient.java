package africa.flot.infrastructure.dayana;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://demo.danaya.africa:8443/api")
@Path("/v2/clients-files")
public interface DanayaClient {

    @POST
    @Path("/upload-files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Uni<Response> uploadDocument(@HeaderParam("Api-Key") String apiKey,
                                 @HeaderParam( "Api-Secret") String apiSecret,
                                 DocumentFormData formData);
}