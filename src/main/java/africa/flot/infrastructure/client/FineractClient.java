package africa.flot.infrastructure.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@Path("/v1")
@RegisterRestClient(configKey = "fineract-api")
@ClientHeaderParam(name = "Fineract-Platform-TenantId", value = "default")
public interface FineractClient {

    @GET
    @Path("/loanproducts/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Response> getLoanProduct(@PathParam("productId") Integer productId);

    @GET
    @Path("/clients/external-id/{externalId}/accounts")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Response> getClientByExternalId(@PathParam("externalId") String externalId);

    @POST
    @Path("/loans")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> createLoan(String requestBody);
}