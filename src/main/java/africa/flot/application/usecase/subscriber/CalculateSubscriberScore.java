package africa.flot.application.usecase.subscriber;

import africa.flot.application.exceptions.SubscriberNotFoundException;
import africa.flot.application.ports.ScoringService;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.infrastructure.repository.SubscripteurRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CalculateSubscriberScore {

    @Inject
    SubscripteurRepository subscriberRepository;

    @Inject
    ScoringService scoringService;
    @WithTransaction
    public Uni<DetailedScore> execute(UUID subscriberId) {
        return subscriberRepository.findById(subscriberId)
                .onItem().ifNull().failWith(new SubscriberNotFoundException("Subscriber not found with id: " + subscriberId))
                .flatMap(scoringService::calculateScore);
    }
}
