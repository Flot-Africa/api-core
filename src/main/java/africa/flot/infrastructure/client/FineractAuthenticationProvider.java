package africa.flot.infrastructure.client;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class FineractAuthenticationProvider implements ClientRequestFilter {

    private static final Logger LOG = Logger.getLogger(FineractAuthenticationProvider.class);

    @ConfigProperty(name = "fineract.api.username")
    String username;

    @ConfigProperty(name = "fineract.api.password")
    String password;

    @PostConstruct
    void init() {
        if (username == null || password == null) {
            throw new IllegalStateException("Les credentials Fineract ne sont pas configurés");
        }
        LOG.debug("FineractAuthenticationProvider initialisé");
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.add("Authorization", "Basic " + encodedAuth);

        // Log pour le debug (à enlever en production)
        LOG.debug("Headers de la requête: "+ headers);
    }
}