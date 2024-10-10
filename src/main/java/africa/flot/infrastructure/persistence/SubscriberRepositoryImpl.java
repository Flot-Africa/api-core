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
public class SubscriberRepositoryImpl implements SubscriberRepository, PanacheRepositoryBase<Subscriber, UUID> {

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
        return PanacheRepositoryBase.super.persistAndFlush(subscriber);
    }

    @WithSession
    public Uni<Subscriber> merge(Subscriber subscriber) {
        return getSession()
                .chain(session -> session.merge(subscriber))
                .chain(this::persistAndFlush);
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

    /**
     * @param phone
     * @return
     */
    @Override
    public Uni<Subscriber> findByPhone(String phone) {
        return Subscriber.find("phone", phone).firstResult();
    }
}