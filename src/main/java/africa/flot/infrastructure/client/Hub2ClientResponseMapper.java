package africa.flot.infrastructure.client;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;

@Provider
public class Hub2ClientResponseMapper implements ResponseExceptionMapper<WebApplicationException> {

    private static final Logger LOG = Logger.getLogger(Hub2ClientResponseMapper.class);

    @Override
    public WebApplicationException toThrowable(Response response) {
        if (response.getStatus() >= 400) {
            String errorBody = response.readEntity(String.class);
            LOG.errorf("HUB2 API error (status %d): %s", response.getStatus(), errorBody);

            // Créer une exception avec le message d'erreur
            Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            String message = String.format("HUB2 API error (status %d): %s", response.getStatus(), errorBody);

            // Recréer la response pour la renvoyer dans l'exception
            Response newResponse = Response
                    .status(response.getStatus())
                    .entity(new ByteArrayInputStream(errorBody.getBytes()))
                    .build();

            return new WebApplicationException(message, newResponse);
        }
        return null;
    }
}