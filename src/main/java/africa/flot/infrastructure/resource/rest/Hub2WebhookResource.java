package africa.flot.infrastructure.resource.rest;

import africa.flot.application.config.Hub2Config;
import africa.flot.application.service.Hub2PaymentService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Path("/webhooks/hub2")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Hub2WebhookResource {

    private static final Logger LOG = Logger.getLogger(Hub2WebhookResource.class);

    @Inject
    Hub2PaymentService paymentService;

    @Inject
    Hub2Config hub2Config;

    @POST
    @Path("/payment")
    public Uni<Response> handlePaymentWebhook(
            @HeaderParam("Hub2-Signature") String signature,
            Map<String, Object> payload) {

        LOG.info("Webhook HUB2 reçu");

        // 1. Vérifier la signature
        if (!verifySignature(payload, signature)) {
            LOG.warn("Signature de webhook HUB2 invalide");
            return Uni.createFrom().item(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // 2. Traiter le payload
        return paymentService.processPaymentWebhook(payload)
                .map(v -> Response.ok().build())
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors du traitement du webhook: %s", throwable.getMessage());
                    // Retourner 200 même en cas d'erreur pour éviter les retentatives
                    return Response.ok().build();
                });
    }

    /**
     * Vérifie la signature du webhook en utilisant le secret fourni
     */
    private boolean verifySignature(Map<String, Object> payload, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            return false;
        }

        try {
            String payloadJson = io.vertx.core.json.JsonObject.mapFrom(payload).encode();

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    hub2Config.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");

            sha256_HMAC.init(secretKey);
            String calculatedSignature = Base64.getEncoder()
                    .encodeToString(sha256_HMAC.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8)));

            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Erreur lors de la vérification de la signature", e);
            return false;
        }
    }
}