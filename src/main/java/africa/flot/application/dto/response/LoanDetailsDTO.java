package africa.flot.application.dto.response;

import africa.flot.domain.model.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LoanDetailsDTO {
    // Informations de base
    private UUID loanId;
    private UUID leadId;
    private UUID vehicleId;
    private BigDecimal principal;
    private BigDecimal weeklyAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LoanStatus status;

    // Progression du paiement
    private BigDecimal totalPaid;
    private BigDecimal outstanding;
    private double paymentProgress;  // Pourcentage
    private int completedPayments;
    private int remainingPayments;
    private LocalDate estimatedEndDate;

    // Statut des échéances
    private LocalDate nextDueDate;
    private LocalDate lastPaymentDate;
    private Integer daysOverdue;
    private Integer weeksOverdue;
    private BigDecimal overdueAmount;
    private UnpaidStatus unpaidStatus;

    // Informations de relance
    private Integer reminderLevel;
    private LocalDate lastReminderDate;
    private LocalDate nextReminderDueDate;

    // Historique récent des paiements
    private List<PaymentSummary> recentPayments;

    @Getter
    @Setter
    public static class PaymentSummary {
        private UUID paymentId;
        private BigDecimal amount;
        private LocalDate paymentDate;
        private LocalDate dueDate;
        private PaymentStatus status;
        private PaymentMethod paymentMethod;
        private Integer daysOverdue;
    }
}