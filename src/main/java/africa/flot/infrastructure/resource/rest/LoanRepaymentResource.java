package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.response.RepaymentResponseDTO;
import africa.flot.application.dto.response.RepaymentTemplateDTO;
import africa.flot.domain.service.LoanRepaymentService;
import africa.flot.infrastructure.security.SecurityService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Repayments", description = "Management of loan repayments")
@Deprecated(since = "1.1.0", forRemoval = true)
public class LoanRepaymentResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger errorf_LOG = Logger.getLogger("errorf");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    LoanRepaymentService repaymentService;

    @Inject
    SecurityService securityService;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/{leadId}/repayment")
    @Operation(summary = "Make a loan repayment",
            description = "If no amount is specified, uses the suggested amount from the template")
    @APIResponse(responseCode = "200", description = "Repayment successfully processed")
    @APIResponse(responseCode = "400", description = "Invalid data provided")
    @APIResponse(responseCode = "404", description = "Loan not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    public Uni<Response> makeRepayment(
            @Parameter(description = "Lead ID", required = true)
            @PathParam("leadId") String leadId,

            @Parameter(description = "Repayment amount (optional)")
            @QueryParam("amount") BigDecimal amount
    ) {
        BUSINESS_LOG.debugf("Repayment request received - Lead: {} - Amount: {}",
                leadId, amount != null ? amount : "not specified");

        return securityService.validateLeadAccess(leadId)
                .chain(() -> repaymentService.makeRepayment(leadId, amount))
                .map(response -> {
                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        return ApiResponseBuilder.failure("Unable to process repayment", Status.BAD_REQUEST);
                    }

                    try {
                        JsonNode responseData = objectMapper.readTree(response.readEntity(String.class));
                        RepaymentResponseDTO dto = new RepaymentResponseDTO(
                                responseData.path("resourceId").asText(),
                                new BigDecimal(responseData.path("changes").path("transactionAmount").asText()),
                                responseData.path("changes").path("transactionDate").asText(),
                                "COMPLETED"
                        );

                        BUSINESS_LOG.infof("Repayment processed - Lead: {} - TransactionId: {}",
                                leadId, dto.transactionId());
                        AUDIT_LOG.infof("Repayment completed - LeadId: {} - Amount: {} - TransactionId: {}",
                                leadId, dto.amount(), dto.transactionId());

                        return ApiResponseBuilder.success(dto);
                    } catch (Exception e) {
                        errorf_LOG.errorf("Error processing repayment response: {}", e.getMessage());
                        return ApiResponseBuilder.failure("Unable to process repayment: "+e.getMessage(), Status.INTERNAL_SERVER_ERROR);
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    errorf_LOG.errorf("Error in repayment process - Lead: {} - Error: {}", leadId, e.getMessage());
                    return ApiResponseBuilder.failure(
                            e instanceof NotFoundException ? "Loan not found": "Unable to process repayment: "+e.getMessage(),
                            e instanceof NotFoundException ? Status.NOT_FOUND : Status.BAD_REQUEST
                    );
                });
    }

    @GET
    @Path("/{leadId}/next-repayment")
    @Operation(summary = "Get next repayment details",
            description = "Returns the suggested amount and other details for the next repayment")
    @APIResponse(responseCode = "200", description = "Details successfully retrieved")
    @APIResponse(responseCode = "404", description = "Loan not found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    @RolesAllowed({"SUBSCRIBER", "ADMIN"})
    public Uni<Response> getNextRepaymentDetails(
            @Parameter(description = "Lead ID", required = true)
            @PathParam("leadId") String leadId
    ) {
        BUSINESS_LOG.debugf("Next repayment details request - Lead: {}", leadId);

        return securityService.validateLeadAccess(leadId)
                .chain(() -> repaymentService.getNextRepaymentDetails(leadId))
                .map(response -> {
                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        return ApiResponseBuilder.failure("Unable to retrieve repayment details", Status.BAD_REQUEST);
                    }

                    try {
                        JsonNode templateData = objectMapper.readTree(response.readEntity(String.class));
                        RepaymentTemplateDTO dto = new RepaymentTemplateDTO(
                                new BigDecimal(templateData.path("amount").asText("0")),
                                new BigDecimal(templateData.path("principalPortion").asText("0")),
                                formatDateArray(templateData.path("date")),
                                templateData.path("currency").path("code").asText("XOF")
                        );

                        BUSINESS_LOG.infof("Next repayment details retrieved - Lead: {} - Amount: {}",
                                leadId, dto.amount());

                        return ApiResponseBuilder.success(dto);
                    } catch (Exception e) {
                        errorf_LOG.errorf("Error processing template response: {}", e.getMessage());
                        return ApiResponseBuilder.failure("Unable to retrieve repayment details: "+e.getMessage(), Status.INTERNAL_SERVER_ERROR);
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    errorf_LOG.errorf("Error retrieving repayment details - Lead: {} - Error: {}", leadId, e.getMessage());
                    return ApiResponseBuilder.failure(
                            e instanceof NotFoundException ? "Loan not found": "Unable to retrieve repayment details: "+e.getMessage(),
                            e instanceof NotFoundException ? Status.NOT_FOUND : Status.BAD_REQUEST
                    );
                });
    }

    private String formatDateArray(JsonNode dateNode) {
        if (!dateNode.isArray() || dateNode.size() < 3) {
            return "";
        }
        return String.format("%d-%02d-%02d",
                dateNode.get(0).asInt(),
                dateNode.get(1).asInt(),
                dateNode.get(2).asInt()
        );
    }
}