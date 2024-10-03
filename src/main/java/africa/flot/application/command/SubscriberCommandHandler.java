package africa.flot.application.command;

import africa.flot.domain.event.SubscriberCreatedEvent;
import africa.flot.domain.model.Subscriber;
import africa.flot.domain.repository.SubscriberRepository;
import africa.flot.infrastructure.messaging.QuarkusEventBus;
import africa.flot.presentation.dto.command.CreateSubscriberDTO;
import africa.flot.presentation.mapper.SubscriberMapper;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class SubscriberCommandHandler {

    @Inject
    SubscriberRepository repository;
    @Inject
    SubscriberMapper mapper;
    @Inject
    QuarkusEventBus eventBus;


    public Uni<UUID> handle(CreateSubscriberCommand command) {
        Subscriber subscriber = mapper.toEntity(command);
        return repository.persist(subscriber)
                .onItem().transform(s -> {
                    eventBus.publish(new SubscriberCreatedEvent(s.id, s.email));
                    return s.id;
                });
    }

    public Uni<Void> verifyKYB(UUID subscriberId) {
        return repository.findById(subscriberId)
                .onItem().ifNotNull().invoke(Subscriber::verifyKYB)
                .onItem().ifNotNull().call(repository::persist).replaceWithVoid();
    }

    public Uni<Void> rejectKYB(UUID subscriberId) {
        return repository.findById(subscriberId)
                .onItem().ifNotNull().invoke(Subscriber::rejectKYB)
                .onItem().ifNotNull().call(repository::persist).replaceWithVoid();
    }

    public Uni<Void> deactivateSubscriber(UUID subscriberId) {
        return repository.findById(subscriberId)
                .onItem().ifNotNull().invoke(Subscriber::deactivate)
                .onItem().ifNotNull().call(repository::persist).replaceWithVoid();
    }
}