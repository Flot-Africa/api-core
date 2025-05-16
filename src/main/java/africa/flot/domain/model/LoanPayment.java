package africa.flot.domain.model;

import africa.flot.domain.model.enums.PaymentStatus;
import africa.flot.domain.model.enums.PaymentMethod;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_payments")
@Getter
@Setter
public class LoanPayment extends PanacheEntityBase {

    @Id
    private UUID id;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    // Relation avec le prêt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", insertable = false, updatable = false)
    private FlotLoan loan;

    // Informations du paiement
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate; // Date à laquelle le paiement a été effectué

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate; // Date d'échéance pour laquelle ce paiement était dû

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    // Calculs de retard
    @Column(name = "days_overdue")
    private Integer daysOverdue = 0; // Combien de jours en retard

    @Column(name = "weeks_overdue")
    private Integer weeksOverdue = 0; // Combien de semaines en retard

    // Référence externe (ID de transaction bancaire, etc.)
    @Column(name = "external_reference")
    private String externalReference;

    // Commentaire ou note sur le paiement
    @Column(name = "notes", length = 500)
    private String notes;

    // Métadonnées
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy; // Qui a enregistré ce paiement (système, admin, etc.)

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        calculatePaymentStatus();
    }

    // Calcul automatique du statut du paiement
    private void calculatePaymentStatus() {
        if (paymentDate == null || dueDate == null) return;

        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(dueDate, paymentDate);
        daysOverdue = Math.max(0, (int) daysDifference);
        weeksOverdue = daysOverdue / 7;

        if (paymentDate.isBefore(dueDate)) {
            status = PaymentStatus.PAID_IN_ADVANCE;
        } else if (paymentDate.equals(dueDate)) {
            status = PaymentStatus.PAID_ON_TIME;
        } else if (daysDifference <= 7) {
            status = PaymentStatus.PAID_LATE_MINOR; // Moins d'une semaine de retard
        } else {
            status = PaymentStatus.PAID_LATE_MAJOR; // Plus d'une semaine de retard
        }
    }

    // Vérification si le paiement couvre l'échéance complète
    public boolean isFullPayment(BigDecimal expectedAmount) {
        return amount.compareTo(expectedAmount) >= 0;
    }

    // Calcul du pourcentage de l'échéance couvert par ce paiement
    public double getCoveragePercentage(BigDecimal expectedAmount) {
        if (expectedAmount.equals(BigDecimal.ZERO)) return 100.0;
        return amount.divide(expectedAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // Vérification si c'est un paiement partiel
    public boolean isPartialPayment(BigDecimal expectedAmount) {
        return amount.compareTo(expectedAmount) < 0;
    }
}