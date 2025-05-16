package africa.flot.domain.model;

import africa.flot.domain.model.enums.LoanStatus;
import africa.flot.domain.model.enums.UnpaidStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flot_loans")
@Getter
@Setter
public class FlotLoan extends PanacheEntityBase {

    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", insertable = false, updatable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicle;

    // Informations du prêt
    @Column(name = "principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal principal; // Montant total du prêt

    @Column(name = "weekly_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal weeklyAmount; // Montant hebdomadaire (principal / 144 semaines)

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // 36 mois après start_date

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status = LoanStatus.ACTIVE;

    // Suivi des paiements
    @Column(name = "total_paid", precision = 15, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "outstanding", precision = 15, scale = 2)
    private BigDecimal outstanding; // Montant restant à payer

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    // Gestion des impayés
    @Column(name = "days_overdue")
    private Integer daysOverdue = 0;

    @Column(name = "weeks_overdue")
    private Integer weeksOverdue = 0;

    @Column(name = "overdue_amount", precision = 10, scale = 2)
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "unpaid_status")
    private UnpaidStatus unpaidStatus = UnpaidStatus.ON_TIME;

    @Column(name = "reminder_level")
    private Integer reminderLevel = 0; // 0 = aucune, 1-4 = niveaux de relance

    @Column(name = "last_reminder_date")
    private LocalDate lastReminderDate;

    @Column(name = "next_reminder_due_date")
    private LocalDate nextReminderDueDate;

    // Métadonnées
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Méthodes utilitaires
    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (outstanding == null) outstanding = principal;
        if (nextDueDate == null) nextDueDate = startDate.plusWeeks(1);
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Calcul du pourcentage payé
    public double getPaymentProgress() {
        if (principal.equals(BigDecimal.ZERO)) return 0.0;
        return totalPaid.divide(principal, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // Vérification si le prêt est en retard
    public boolean isOverdue() {
        return nextDueDate.isBefore(LocalDate.now()) && outstanding.compareTo(BigDecimal.ZERO) > 0;
    }

    // Calcul du nombre de paiements restants
    public int getRemainingPayments() {
        return outstanding.divide(weeklyAmount, 0, java.math.RoundingMode.CEILING).intValue();
    }

    // Calcul du nombre de paiements effectués
    public int getCompletedPayments() {
        return totalPaid.divide(weeklyAmount, 0, java.math.RoundingMode.DOWN).intValue();
    }

    // Vérification si le prêt est complètement remboursé
    public boolean isFullyPaid() {
        return outstanding.compareTo(BigDecimal.ZERO) <= 0;
    }

    // Calcul de la date de fin théorique basée sur les paiements
    public LocalDate getEstimatedEndDate() {
        if (isFullyPaid()) return LocalDate.now();
        int remainingPayments = getRemainingPayments();
        return nextDueDate.plusWeeks(remainingPayments - 1);
    }
}