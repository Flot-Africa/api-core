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

    @Column(nullable = false, unique = true)
    public String username;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "last_login")
    public LocalDate lastLogin;
    @Column(name = "fineract_client_id")
    private Integer fineractClientId;

    @Column(name = "pending_welcome_sms")
    private boolean pendingWelcomeSms = false;

    @Column(name = "sms_retry_count")
    private int smsRetryCount = 0;

    @Column(name = "temporary_password")
    private String temporaryPassword;
    // Méthodes utilitaires
    public void deactivate() {
        this.isActive = false;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDate.now();
    }
}
