package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.request.PayementIntentRequest;
import africa.flot.application.service.Hub2ServiceTest;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import java.math.BigDecimal;

@Path("/test") // Changé pour correspondre à la nouvelle URL
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Flot Loans", description = "APIs pour la gestion des prêts Flot")
public class Hub2SRessourceTest {

    private static final Logger LOG = Logger.getLogger(Hub2SRessourceTest.class);
    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    Hub2ServiceTest hub2PaymentService;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/payments")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Enregistrer un paiement", description = "Traite un paiement pour un prêt donné")
    @APIResponse(responseCode = "200", description = "Paiement traité avec succès")
    @APIResponse(responseCode = "500", description = "Erreur lors du traitement du paiement")
    public Uni<Response> processPaymentTest(MobilePaymentRequest paymentData) {
        try {
            // Logs détaillés
            LOG.infof("Requête reçue: msisdn=%s, provider=%s, amount=%d",
                    paymentData.getMsisdn(), paymentData.getProvider(), paymentData.getAmount());

            // Nettoyer le msisdn
            String cleanMsisdn = paymentData.getMsisdn() != null ? paymentData.getMsisdn().trim() : "";

            PayementIntentRequest request = PayementIntentRequest.builder()
                    .customerReference("CUSTOMER_" + cleanMsisdn)
                    .purchaseReference("PAYMENT_" + System.currentTimeMillis())
                    .amount(new BigDecimal(paymentData.getAmount()))
                    .currency("XOF")
                    .build();

            LOG.infof("PaymentIntentRequest créé: ref=%s, montant=%s",
                    request.getCustomerReference(), request.getAmount());

            // Vérification que le service hub2PaymentService n'est pas null
            if (hub2PaymentService == null) {
                LOG.error("hub2PaymentService est null - erreur d'injection");
                return Uni.createFrom().item(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(ApiResponseBuilder.failure("Erreur de configuration: service non disponible",
                                Response.Status.INTERNAL_SERVER_ERROR))
                        .build());
            }

            return hub2PaymentService.repayLoanTest(request)
                    .map(paymentResponse -> {
                        AUDIT_LOG.infof("Paiement effectué avec succès - ID: %s", paymentResponse.getId());
                        return Response.ok(ApiResponseBuilder.success("Paiement effectué avec succès", Response.Status.ACCEPTED)).build();
                    })
                    .onFailure().recoverWithItem(e -> {
                        // Log complet de l'exception avec stack trace
                        ERROR_LOG.error("Erreur détaillée lors du traitement du paiement:", e);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(ApiResponseBuilder.failure("Erreur lors du traitement du paiement: " + e.getMessage(),
                                        Response.Status.INTERNAL_SERVER_ERROR))
                                .build();
                    });
        } catch (Exception e) {
            // Log complet de l'exception avec stack trace
            ERROR_LOG.error("Exception non gérée dans le contrôleur:", e);
            return Uni.createFrom().item(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponseBuilder.failure("Erreur de traitement: " + e.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR))
                    .build());
        }
    }





    // Classe pour recevoir la requête de paiement mobile
    public static class MobilePaymentRequest {
        private String msisdn;
        private String provider;
        private Integer amount;
        private String otp;

        // Constructeur par défaut nécessaire pour la désérialisation
        public MobilePaymentRequest() {}

        // Getters et setters
        public String getMsisdn() { return msisdn; }
        public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }

        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }

        @Override
        public String toString() {
            return "MobilePaymentRequest{" +
                    "msisdn='" + msisdn + '\'' +
                    ", provider='" + provider + '\'' +
                    ", amount=" + amount +
                    ", otp='" + otp + '\'' +
                    '}';
        }
    }
}