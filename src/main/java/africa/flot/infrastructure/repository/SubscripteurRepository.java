package africa.flot.infrastructure.repository;

import africa.flot.domain.model.Subscriber;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;


public interface SubscripteurRepository extends PanacheRepositoryBase<Subscriber, UUID> {

}
