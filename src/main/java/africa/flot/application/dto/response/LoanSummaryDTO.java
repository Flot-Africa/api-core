package africa.flot.application.dto.response;

import africa.flot.domain.model.enums.LoanStatus;
import africa.flot.domain.model.enums.UnpaidStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class LoanSummaryDTO {
    private UUID loanId;
    private UUID leadId;
    private String driverName;
    private String vehicleModel;
    private BigDecimal weeklyAmount;
    private LocalDate nextDueDate;
    private BigDecimal overdueAmount;
    private Integer daysOverdue;
    private UnpaidStatus unpaidStatus;
    private Integer reminderLevel;
    private LoanStatus status;
    private double paymentProgress;
}
