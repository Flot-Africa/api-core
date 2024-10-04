package africa.flot.domain.model;

import africa.flot.domain.model.exception.BusinessException;
import africa.flot.domain.model.enums.LoanStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan")
public class Loan extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "subscriber_id", nullable = false)
    public Subscriber subscriber;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    public Vehicle vehicle;

    @Column(nullable = false)
    public Double amount;

    @Column(name = "interest_rate", nullable = false)
    public Double interestRate;

    @Column(name = "term_months", nullable = false)
    public Integer termMonths;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public LoanStatus status = LoanStatus.PENDING;

    @Column(name = "monthly_payment", nullable = false)
    public Double monthlyPayment;

    @Column(name = "total_amount", nullable = false)
    public Double totalAmount;

    // Business methods
    public void calculatePaymentSchedule() {
        double monthlyRate = (interestRate / 100) / 12;
        this.monthlyPayment = (amount * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -termMonths));
        this.totalAmount = this.monthlyPayment * termMonths;
    }

    public void activateLoan() {
        if (this.status != LoanStatus.PENDING) {
            throw new BusinessException("Loan cannot be activated from its current status.");
        }
        this.status = LoanStatus.ACTIVE;
    }

    public void markAsRepaid() {
        if (this.status != LoanStatus.ACTIVE) {
            throw new BusinessException("Only active loans can be marked as repaid.");
        }
        this.status = LoanStatus.REPAID;
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        this.startDate = LocalDate.now();
        this.endDate = this.startDate.plusMonths(termMonths);
        calculatePaymentSchedule();
    }
}
