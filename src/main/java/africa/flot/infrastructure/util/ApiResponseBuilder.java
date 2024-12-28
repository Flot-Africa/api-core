package africa.flot.infrastructure.util;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.logging.Logger;
import java.util.HashMap;
import java.util.Map;

public class ApiResponseBuilder {

    private static final Logger LOG = Logger.getLogger(ApiResponseBuilder.class);

    public static Response success(Object data) {
        LOG.info("ApiResponseBuilder.success: Building success response with data: " + data);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return Response.ok(response).build();
    }

    public static Response success(Object data, Status status) {
        LOG.info("ApiResponseBuilder.success: Building success response with data: " + data);
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("data", data);
        return Response.ok(response).build();
    }


    public static Response success() {
        LOG.info("ApiResponseBuilder.success: Building success response without data");
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return Response.ok(response).build();
    }

    public static Response failure(String message, Status status) {
        LOG.error("ApiResponseBuilder.failure: Building failure response with message: " + message + ", status: " + status);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failure");
        response.put("message", message);
        return Response.status(status).entity(response).build();
    }
}
