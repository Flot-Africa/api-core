package africa.flot.domain.model;

import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.model.enums.VehicleStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class Vehicle extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(nullable = false)
    public String model;

    @Column(nullable = false)
    public String brand;

    @Column(name = "serial_number", nullable = false, unique = true)
    public String serialNumber;

    @Column(name = "license_plate", nullable = false, unique = true)
    public String licensePlate;

    @Column(name = "service_start_date", nullable = false)
    public LocalDate serviceStartDate;

    @Column(name = "energy_type", nullable = false)
    public String energyType;

    @Column(name = "battery_capacity", nullable = false)
    public Double batteryCapacity;

    @Column(name = "theoretical_range", nullable = false)
    public Integer theoreticalRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "last_maintenance_date")
    public LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    public LocalDate nextMaintenanceDate;

    @Column(name = "total_mileage", nullable = false)
    public Double totalMileage = 0.0;

    @Column(name = "gps_tracker_id")
    public String gpsTrackerId;

    // Business methods
    public void assignToSubscriber(UUID subscriberId) {
        if (this.status != VehicleStatus.AVAILABLE) {
            throw new BusinessException("Vehicle is not available for assignment.");
        }
        this.status = VehicleStatus.RENTED;
        // Additional logic for assignment can be added here
    }

    public void scheduleMaintenance() {
        this.status = VehicleStatus.MAINTENANCE;
    }

    public void markAsAvailable() {
        this.status = VehicleStatus.AVAILABLE;
    }
}
