package africa.flot.application.dto.response;


import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class Hub2ClientResponseMapper implements ResponseExceptionMapper<RuntimeException> {
    @Override
    public RuntimeException toThrowable(Response response) {
        String message = response.readEntity(String.class);
        return new WebApplicationException("Erreur HUB2: " + message, response);
    }
}
