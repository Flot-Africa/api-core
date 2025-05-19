package africa.flot.infrastructure.resource.rest;

// --- Webhook Controller ---

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/webhooks/hub2/payment")
@Consumes(MediaType.APPLICATION_JSON)
public class Hub2WebhookResource {

    @POST
    public Response onWebhook(JsonObject payload) {
        String event = payload.getString("event");
        JsonObject data = payload.getJsonObject("data");

        if ("payment.succeeded".equals(event)) {
            String reference = data.getString("purchaseReference");
            // Extract loanId from reference: "loan_1234_2024-07-15"
            Long loanId = Long.parseLong(reference.split("_")[1]);
            // Update loan status, save transaction, etc.
        }

        return Response.ok().build();
    }
}
