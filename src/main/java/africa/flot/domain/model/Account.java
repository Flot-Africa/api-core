package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "accounts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Account extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @OneToOne
    @JoinColumn(name = "lead_id", unique = true)
    public Lead lead;

    @ManyToOne
    @JoinColumn(name = "package_id")
    public Package subscribedPackage;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "last_login")
    public LocalDate lastLogin;

    // Méthodes utilitaires
    public void deactivate() {
        this.isActive = false;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDate.now();
    }
}
