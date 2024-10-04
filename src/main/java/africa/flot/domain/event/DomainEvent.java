package africa.flot.domain.event;


import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;


public abstract class DomainEvent implements Serializable {

    private final UUID eventId;
    private final Instant occurredOn;

    public DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }
}
