package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "scorings")
public class Scoring extends PanacheEntityBase {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "key_form_id", nullable = false)
    private UUID keyFormId;

    @NotNull
    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "note", length = Integer.MAX_VALUE)
    private String note;

    @Column(name = "folder", length = Integer.MAX_VALUE)
    private String folder;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "status", nullable = false)
    private Boolean status = false;

    @Size(max = 255)
    @Column(name = "client_number")
    private String clientNumber;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
