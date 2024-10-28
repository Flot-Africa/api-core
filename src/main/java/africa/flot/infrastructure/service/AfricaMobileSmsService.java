package africa.flot.infrastructure.service;

import africa.flot.application.ports.SmsService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AfricaMobileSmsService implements SmsService {

    private static final Logger LOG = Logger.getLogger(AfricaMobileSmsService.class);

    @ConfigProperty(name = "africa.mobile.accountid")
    String accountId;

    @ConfigProperty(name = "africa.mobile.password")
    String password;

    @ConfigProperty(name = "africa.mobile.sender")
    String sender;

    @Inject
    @RestClient
    SmsClient smsClient;

    @Inject
    Vertx vertx;

    @Override
    public Uni<Response> sendSMS(String phoneNumber, String message) {
        SmsRequest smsRequest = new SmsRequest(accountId, password, sender, message, phoneNumber);

        // Créer un Future qui sera complété sur un worker thread
        return Uni.createFrom().emitter(em -> {
            vertx.<Response>executeBlocking(promise -> {
                try {
                    // Effectuer l'appel REST sur un worker thread
                    smsClient.sendSms(smsRequest)
                            .subscribe().with(
                                    response -> {
                                        if (response.getStatus() == 200) {
                                            LOG.info("SMS envoyé avec succès à " + phoneNumber);
                                            promise.complete(response);
                                        } else {
                                            LOG.error("Échec de l'envoi du SMS. Statut: " + response.getStatus());
                                            promise.complete(response); // On retourne quand même la réponse pour gérer l'erreur en amont
                                        }
                                    },
                                    error -> {
                                        LOG.error("Erreur lors de l'envoi du SMS", error);
                                        promise.fail(error);
                                    }
                            );
                } catch (Exception e) {
                    promise.fail(e);
                }
            }, ar -> {
                if (ar.succeeded()) {
                    em.complete(ar.result());
                } else {
                    em.fail(ar.cause());
                }
            });
        });
    }

    @RegisterRestClient(configKey = "africa-mobile-api")
    public interface SmsClient {
        @POST
        @Path("/api")
        @Produces(MediaType.APPLICATION_JSON)
        Uni<Response> sendSms(SmsRequest request);
    }

    public static class SmsRequest {
        public String accountid;
        public String password;
        public String sender;
        public String text;
        public String to;

        public SmsRequest(String accountid, String password, String sender, String text, String to) {
            this.accountid = accountid;
            this.password = password;
            this.sender = sender;
            this.text = text;
            this.to = to;
        }
    }
}