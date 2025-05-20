package africa.flot.application.client;

import africa.flot.application.dto.response.Hub2ClientResponseMapper;
import africa.flot.application.dto.response.IntentResponse;
import africa.flot.application.dto.response.PaymentResponse;
import africa.flot.application.service.Hub2HeadersProvider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@RegisterRestClient(configKey = "hub2-api")
@RegisterProvider(Hub2ClientResponseMapper.class)
@RegisterProvider(Hub2HeadersProvider.class)
@Path("/")
public interface Hub2Client {
    @POST
    @Path("payment-intents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    IntentResponse createPaymentIntent(Map<String, Object> body);

    @POST
    @Path("payment-intents/{id}/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    PaymentResponse payWithMobileMoney(@HeaderParam("merchantId") Map<String, String> headers,
                                     @PathParam("id") String id,
                                     Map<String, Object> body);

    @POST
    @Path("payment-intents/{id}/authentication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void authenticate(@HeaderParam("merchantId") Map<String, String> headers,
                     @PathParam("id") String id,
                     Map<String, Object> body);
}