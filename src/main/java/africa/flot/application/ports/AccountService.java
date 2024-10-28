package africa.flot.application.ports;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

public interface AccountService {
    Uni<Response> subscribeToPackage(UUID leadId, UUID packageId);
}

