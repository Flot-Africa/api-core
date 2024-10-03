package africa.flot.domain.repository;


import africa.flot.domain.model.Vehicle;
import africa.flot.domain.model.enums.VehicleStatus;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VehicleRepository implements PanacheRepositoryBase<Vehicle, UUID> {

    // Additional query methods

    public Uni<List<Vehicle>> findAllAvailable() {
        return list("status", VehicleStatus.AVAILABLE);
    }

    public Uni<Vehicle> findByLicensePlate(String licensePlate) {
        return find("licensePlate", licensePlate).firstResult();
    }
}
