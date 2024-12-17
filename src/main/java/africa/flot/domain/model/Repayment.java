package africa.flot.domain.model;


import africa.flot.domain.model.enums.PaymentMethode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "Repayment")
public class Repayment {
    @Id
    private UUID RepaymentID;
    private BigDecimal amountToBeDeposeted;
    private BigDecimal amountDeposited;
    private Instant dueDate;
    private Instant paymentDate;
    private PaymentMethode paymentMethode;
    private UUID loadID;
    private UUID transactionID;

}
