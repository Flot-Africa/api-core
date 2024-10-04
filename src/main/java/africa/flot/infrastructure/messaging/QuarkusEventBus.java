package africa.flot.infrastructure.messaging;

import africa.flot.domain.event.DomainEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.vertx.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class QuarkusEventBus {

    private static final String DOMAIN_EVENTS_ADDRESS = "domain-events";

    @Inject
    EventBus eventBus;

    public void publish(DomainEvent event) {
        eventBus.publish(DOMAIN_EVENTS_ADDRESS, event);
    }

    public Multi<DomainEvent> subscribe() {
        return Multi.createFrom().emitter(emitter -> {
            eventBus.consumer(DOMAIN_EVENTS_ADDRESS, message -> {
                emitter.emit((DomainEvent) message.body());
            });
        });
    }

    @ConsumeEvent(DOMAIN_EVENTS_ADDRESS)
    public void consume(DomainEvent event) {
        // Cette méthode sera appelée pour chaque événement publié.
        // Vous pouvez ajouter ici une logique générale de traitement des événements si nécessaire.
        System.out.println("Event received: " + event.getClass().getSimpleName());
    }
}