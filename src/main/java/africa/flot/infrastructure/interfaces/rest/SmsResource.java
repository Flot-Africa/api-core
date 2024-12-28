package africa.flot.infrastructure.interfaces.rest;

import africa.flot.domain.model.Account;
import africa.flot.infrastructure.service.JetfySmsService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/sms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SMS Management", description = "APIs for SMS balance retrieval and sending SMS messages")
public class SmsResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    JetfySmsService smsService;

    /**
     * Endpoint to retrieve SMS balance.
     */
    @GET
    @Path("/balance")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Get SMS balance", description = "Retrieve the current SMS balance.")
    @APIResponse(
            responseCode = "200",
            description = "SMS balance retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Error while retrieving SMS balance",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> getSmsBalance() {
        BUSINESS_LOG.info("Fetching SMS balance");
        return smsService.getSmsBalance()
                .map(balance -> {
                    AUDIT_LOG.info("SMS balance retrieved successfully");
                    return ApiResponseBuilder.success(Map.of(
                            "balance", balance,
                            "currency", "XOF",
                            "sms_count", balance / JetfySmsService.COST_PER_SMS
                    ));
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Error while retrieving SMS balance: ", throwable);
                    return ApiResponseBuilder.failure(
                            "Error while retrieving SMS balance",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    /**
     * Endpoint to send SMS.
     */
    @POST
    @Path("/send")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Send SMS", description = "Send an SMS to a specified phone number.")
    @APIResponse(
            responseCode = "200",
            description = "SMS sent successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Error while sending SMS",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Uni<Response> sendSms(
            @Valid @RequestBody(description = "Details of the SMS to be sent", required = true) SendSmsRequest request) {
        BUSINESS_LOG.info("Attempting to send SMS to " + request.phoneNumber);

        // Temporary account creation for testing
        Account tempAccount = new Account();
        tempAccount.setUsername(request.phoneNumber);

        return smsService.sendSMS(request.phoneNumber, request.message, tempAccount)
                .map(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        AUDIT_LOG.info("SMS sent successfully to " + request.phoneNumber);
                        return ApiResponseBuilder.success(Map.of(
                                "message", "SMS sent successfully",
                                "recipient", request.phoneNumber,
                                "smsCount", calculateSmsCount(request.message)
                        ));
                    } else {
                        ERROR_LOG.warn("Failed to send SMS to " + request.phoneNumber);
                        return ApiResponseBuilder.failure(
                                "Failed to send SMS",
                                Response.Status.fromStatusCode(response.getStatus())
                        );
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.error("Error while sending SMS to " + request.phoneNumber, throwable);
                    return ApiResponseBuilder.failure(
                            "Error while sending SMS: " + throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    /**
     * Calculate the number of SMS messages required based on message length.
     */
    private int calculateSmsCount(String message) {
        return (message.length() + 159) / 160;
    }

    /**
     * Request payload for sending SMS.
     */
    public static class SendSmsRequest {
        @NotBlank(message = "Phone number is required")
        public String phoneNumber;

        @NotBlank(message = "Message is required")
        public String message;
    }
}
