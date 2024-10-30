package africa.flot.infrastructure.repository.impl;

import africa.flot.application.ports.OldPasswordRepository;
import africa.flot.domain.model.OldPassword;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OldPasswordRepositoryImpl implements OldPasswordRepository, PanacheRepositoryBase<OldPassword, UUID> {

    @Override
    @WithSession
    public Uni<Boolean> isPasswordUsed(UUID accountId, String passwordHash) {
        return OldPassword.find("account.id = ?1 and passwordHash = ?2", accountId, passwordHash)
                .firstResult()
                .map(Objects::nonNull);
    }


    @Override
    @WithSession
    public Uni<Optional<OldPassword>> findByAccountAndHash(UUID accountId, String passwordHash) {
        return OldPassword.<OldPassword>find("account.id = ?1 and passwordHash = ?2", accountId, passwordHash)
                .firstResult()
                .map(Optional::ofNullable);
    }

    @Override
    @WithSession
    public Uni<Void> saveOldPassword(OldPassword oldPasswordEntity) {
        return oldPasswordEntity.persistAndFlush()
                .replaceWith(Uni.createFrom().voidItem());
    }


}

