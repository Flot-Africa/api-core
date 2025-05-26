package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_payment_intents")
@Getter
@Setter
public class LeadPaymentIntent extends PanacheEntityBase {

    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "hub2_intent_id")
    private String hub2IntentId;

    @Column(name = "hub2_token")
    private String hub2Token;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency = "XOF";

    @Column(name = "status")
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "provider")
    private String provider; // "orange", "mtn", etc.

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}