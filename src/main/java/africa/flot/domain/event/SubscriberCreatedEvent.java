package africa.flot.domain.event;

import java.util.UUID;

public class SubscriberCreatedEvent extends DomainEvent {

    private final UUID subscriberId;
    private final String email;

    public SubscriberCreatedEvent(UUID subscriberId, String email) {
        super();
        this.subscriberId = subscriberId;
        this.email = email;
    }

    public UUID getSubscriberId() {
        return subscriberId;
    }

    public String getEmail() {
        return email;
    }
}