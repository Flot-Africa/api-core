package africa.flot.infrastructure.persistence;

import africa.flot.domain.model.Subscriber;
import africa.flot.domain.repository.SubscriberRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PostgresSubscriberRepository implements SubscriberRepository, PanacheRepositoryBase<Subscriber, UUID> {

    @Override
    public Uni<Subscriber> findById(UUID id) {
        return PanacheRepositoryBase.super.findById(id);
    }

    @Override
    public Uni<List<Subscriber>> listAll() {
        return PanacheRepositoryBase.super.listAll();
    }

    @Override
    @WithSession
    public Uni<Subscriber> persist(Subscriber subscriber) {
        return PanacheRepositoryBase.super.persistAndFlush(subscriber);
    }

    @Override
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