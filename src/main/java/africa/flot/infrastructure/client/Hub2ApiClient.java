package africa.flot.infrastructure.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

//@RegisterRestClient(configKey = "hub2-api")
@RegisterRestClient(baseUri = "https://api.hub2.com")
@Path("/")
public interface Hub2ApiClient {

    @POST
    @Path("payment-intents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Map<String, Object>> createPaymentIntent(
            @HeaderParam("ApiKey") String apiKey,
            @HeaderParam("merchantId") String merchantId,
            @HeaderParam("environment") String environment,
            Map<String, Object> payload);

    @POST
    @Path("payment-intents/{id}/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Map<String, Object>> initiatePayment(
            @HeaderParam("ApiKey") String apiKey,
            @HeaderParam("merchantId") String merchantId,
            @HeaderParam("environment") String environment,
            @PathParam("id") String id,
            Map<String, Object> payload);

    @POST
    @Path("payment-intents/{id}/authentication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Map<String, Object>> completeAuthentication(
            @HeaderParam("ApiKey") String apiKey,
            @HeaderParam("merchantId") String merchantId,
            @HeaderParam("environment") String environment,
            @PathParam("id") String id,
            Map<String, Object> payload);

    @GET
    @Path("payment-intents/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Map<String, Object>> getPaymentIntent(
            @HeaderParam("ApiKey") String apiKey,
            @HeaderParam("merchantId") String merchantId,
            @HeaderParam("environment") String environment,
            @PathParam("id") String id,
            @QueryParam("token") String token);

    @POST
    @Path("webhooks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Map<String, Object>> registerWebhook(
            @HeaderParam("ApiKey") String apiKey,
            @HeaderParam("merchantId") String merchantId,
            @HeaderParam("environment") String environment,
            Map<String, Object> payload);
}