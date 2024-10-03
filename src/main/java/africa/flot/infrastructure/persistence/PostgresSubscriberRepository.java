package africa.flot.infrastructure.persistence;

import africa.flot.domain.model.Subscriber;
import africa.flot.domain.repository.SubscriberRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PostgresSubscriberRepository implements SubscriberRepository, PanacheRepositoryBase<Subscriber, UUID> {

    @WithSession
    public Uni<Subscriber> findById(UUID id) {
        return Subscriber.findById(id);
    }

    @WithSession
    public Uni<List<Subscriber>> listAll() {
        return Subscriber.listAll();
    }

    @WithSession
    public Uni<Subscriber> persist(Subscriber subscriber) {
        return subscriber.persistAndFlush();
    }

    @Override
    @WithSession
    public Uni<Subscriber> findByEmail(String email) {
        return find("email", email).firstResult();
    }

    @Override
    public Uni<Subscriber> findByDriverLicenseNumber(String driverLicenseNumber) {
        return find("driverLicenseNumber", driverLicenseNumber).firstResult();
    }

    @Override
    public Uni<Boolean> deleteById(UUID id) {
        return PanacheRepositoryBase.super.deleteById(id);
    }
}