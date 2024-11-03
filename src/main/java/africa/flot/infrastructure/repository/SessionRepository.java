package africa.flot.infrastructure.repository;

import africa.flot.domain.model.Session;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionRepository implements PanacheRepository<Session> {

    public Uni<Session> findBySessionId(String sessionId) {
        return find("id", sessionId).firstResult();
    }
}