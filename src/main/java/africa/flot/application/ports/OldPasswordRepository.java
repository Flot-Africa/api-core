package africa.flot.application.ports;

import africa.flot.domain.model.OldPassword;
import io.smallrye.mutiny.Uni;

import java.util.Optional;
import java.util.UUID;


public interface OldPasswordRepository {
    Uni<Boolean> isPasswordUsed(UUID accountId, String passwordHash);
    Uni<Optional<OldPassword>> findByAccountAndHash(UUID accountId, String passwordHash);

    Uni<Void> saveOldPassword(OldPassword oldPasswordEntity);
}



