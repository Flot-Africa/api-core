package africa.flot.infrastructure.client;

import africa.flot.infrastructure.service.JetfySmsService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "jetfy-api")
public interface JetfyClient {
    @POST
    @Path("/api/v1/sms/send")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<JetfySmsService.ApiResponse> sendSms(
            @HeaderParam("Authorization") String authHeader,
            JetfySmsService.SmsRequest request
    );

    @GET
    @Path("/api/v1/balance/sms_module")
    Uni<JetfySmsService.BalanceResponse> getBalance(
            @HeaderParam("Authorization") String authHeader
    );
}