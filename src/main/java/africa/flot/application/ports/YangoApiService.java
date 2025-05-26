package africa.flot.application.ports;

import africa.flot.domain.model.YangoDriverData;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface YangoApiService {
    Uni<YangoDriverData> getDriverInfo(UUID leadId);
}