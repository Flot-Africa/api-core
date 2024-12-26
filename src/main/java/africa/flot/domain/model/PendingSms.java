package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class PendingSms extends PanacheEntity {

    @NotNull
    private String phoneNumber;

    @NotNull
    private String message;

    @NotNull
    private UUID accountId;

    public PendingSms() {
    }

    public PendingSms(String phoneNumber, String message, UUID accountId) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.accountId = accountId;
    }
}
