package africa.flot.domain.event;

import java.util.UUID;

public class VehicleAssignedEvent extends DomainEvent {

    private final UUID vehicleId;
    private final UUID subscriberId;

    public VehicleAssignedEvent(UUID vehicleId, UUID subscriberId) {
        super();
        this.vehicleId = vehicleId;
        this.subscriberId = subscriberId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public UUID getSubscriberId() {
        return subscriberId;
    }
}
