package africa.flot.infrastructure.repository;

import africa.flot.domain.model.User;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface UserRepository {
    Uni<User> findById(Long id);
    Uni<List<User>> listAll();
    Uni<User> persist(User user);
    Uni<User> findByEmail(String email);
    Uni<Boolean> deleteById(Long id);
    Uni<User> findByRememberToken(String rememberToken);
}