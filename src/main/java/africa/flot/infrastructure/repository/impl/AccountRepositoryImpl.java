package africa.flot.infrastructure.repository.impl;

import africa.flot.domain.model.Account;
import africa.flot.infrastructure.repository.AccountRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class AccountRepositoryImpl implements AccountRepository, PanacheRepositoryBase<Account, UUID> {

    @Override
    public Uni<Account> findById(UUID id) {
        return findById(id);
    }

    @Override
    public Uni<Account> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    @Override
    public Uni<Boolean> existsByUsername(String username) {
        return count("username", username)
                .map(count -> count > 0);
    }

    @Override
    @WithSession
    public Uni<Account> persist(Account account) {
        return persistAndFlush(account);
    }


    @Override
    public Uni<Boolean> deleteById(UUID id) {
        return PanacheRepositoryBase.super.deleteById(id);
    }

    @Override
    public Uni<Void> deactivateBySubscriberId(UUID subscriberId) {
        return update("isActive = false where subscriber.id = ?1", subscriberId)
                .replaceWithVoid();
    }
}