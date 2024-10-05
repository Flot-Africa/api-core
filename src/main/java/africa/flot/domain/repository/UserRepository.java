package africa.flot.domain.repository;

import africa.flot.domain.model.User;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface UserRepository {
    Uni<User> findById(UUID id);
    Uni<List<User>> listAll();
    Uni<User> persist(User user);
    Uni<User> findByEmail(String email);
    Uni<Boolean> deleteById(UUID id);
    Uni<User> findByRememberToken(String rememberToken);
}