package africa.flot.infrastructure.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Provider
public class FineractLoggingFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final Logger LOG = Logger.getLogger(FineractLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        LOG.infof("Fineract Request - Method: %s, URL: %s",
                requestContext.getMethod(),
                requestContext.getUri().toString());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        // Lire le corps de la réponse
        String responseBody = readResponseBody(responseContext);

        LOG.infof("Fineract Response - URL: %s, Status: %d, Response: %s",
                requestContext.getUri().toString(),
                responseContext.getStatus(),
                responseBody);
    }

    private String readResponseBody(ClientResponseContext responseContext) throws IOException {
        if (responseContext.hasEntity()) {
            try (InputStream inputStream = responseContext.getEntityStream()) {
                byte[] bytes = inputStream.readAllBytes();
                // Recréer le stream pour ne pas perturber les autres traitements
                responseContext.setEntityStream(new ByteArrayInputStream(bytes));
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return "No response body";
    }
}
