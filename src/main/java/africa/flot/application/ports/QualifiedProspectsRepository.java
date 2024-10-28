package africa.flot.application.ports;


import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface QualifiedProspectsRepository {
    public Uni<Boolean> isQualified(UUID leadId);
}

