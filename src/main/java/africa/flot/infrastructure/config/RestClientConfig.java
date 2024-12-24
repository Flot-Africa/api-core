package africa.flot.infrastructure.config;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import africa.flot.infrastructure.client.FineractClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.URI;

@ApplicationScoped
@RegisterForReflection
public class RestClientConfig {

    @ConfigProperty(name = "fineract.api.url")
    String fineractUrl;

    @Produces
    @ApplicationScoped
    public FineractClient produceFineractClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(fineractUrl))
                .hostnameVerifier(new NoopHostnameVerifier())
                .build(FineractClient.class);
    }
}