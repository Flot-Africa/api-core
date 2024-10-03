package africa.flot.domain.repository;

import africa.flot.domain.model.Subscriber;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface SubscriberRepository {
    Uni<Subscriber> findById(UUID id);
    Uni<List<Subscriber>> listAll();  // Changé de findAll à listAll
    Uni<Subscriber> persist(Subscriber subscriber);
    Uni<Subscriber> findByEmail(String email);
    Uni<Subscriber> findByDriverLicenseNumber(String driverLicenseNumber);
    Uni<Boolean> deleteById(UUID id);
}