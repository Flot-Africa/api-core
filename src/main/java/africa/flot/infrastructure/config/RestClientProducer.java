package africa.flot.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

@ApplicationScoped
public class RestClientProducer {

    @Produces
    @ApplicationScoped
    public Client produceRestClient() {
        return ClientBuilder.newClient();
    }
}