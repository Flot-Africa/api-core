package africa.flot.infrastructure.repository;

import africa.flot.domain.model.Account;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface AccountRepository  {

    Uni<Account> findById(UUID id);

     Uni<Void> updatePassword(UUID accountId, String newPasswordHash);

    Uni<Account> findByUsername(String username);

    Uni<Boolean> existsByUsername(String username);
    Uni<Account> persist(Account account);

    Uni<Boolean> deleteById(UUID id);

    Uni<Void> deactivateBySubscriberId(UUID subscriberId);
}