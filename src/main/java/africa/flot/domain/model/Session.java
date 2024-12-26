package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Table(name = "sessions")
@Immutable
@Setter
@Getter
public class Session extends PanacheEntityBase {

    @Id
    public String id;
    @Column(name = "user_id")
    public Long userId;
    @Column(name = "ip_address")
    public String ipAddress;
    @Column(name = "user_agent")
    public String userAgent;
    @Column(name = "payload")
    public String payload;
    @Column(name = "last_activity")
    public Integer lastActivity; // Timestamp de la dernière activité

    public boolean isActive(int sessionTimeout) {
        long currentTimeInSeconds = Instant.now().getEpochSecond();
        return (currentTimeInSeconds - lastActivity) <= sessionTimeout;
    }
}