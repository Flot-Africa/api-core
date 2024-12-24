package africa.flot.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Base64;

@ApplicationScoped
public class FineractAuthenticationProvider implements ClientRequestFilter {

    @ConfigProperty(name = "fineract.api.username")
    String username;

    @ConfigProperty(name = "fineract.api.password")
    String password;

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        String auth = username + ":" + password;
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
        headers.add("Authorization", authHeader);
    }
}