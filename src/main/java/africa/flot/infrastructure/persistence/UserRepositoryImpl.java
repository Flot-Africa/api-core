package africa.flot.infrastructure.persistence;

import africa.flot.domain.model.User;
import africa.flot.domain.repository.UserRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<User, UUID> {

    @WithSession
    public Uni<User> findById(UUID id) {
        return User.findById(id);
    }

    @WithSession
    public Uni<List<User>> listAll() {
        return User.listAll();
    }

    @WithSession
    public Uni<User> persist(User user) {
        return PanacheRepositoryBase.super.persistAndFlush(user);
    }

    @Override
    @WithSession
    public Uni<User> findByEmail(String email) {
        return find("email", email).firstResult();
    }

    @Override
    public Uni<Boolean> deleteById(UUID id) {
        return PanacheRepositoryBase.super.deleteById(id);
    }

    @Override
    @WithSession
    public Uni<User> findByRememberToken(String rememberToken) {
        return find("rememberToken", rememberToken).firstResult();
    }
}