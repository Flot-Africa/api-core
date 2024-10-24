package africa.flot.application.command.loancommande;


import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.Vehicle;
import africa.flot.domain.model.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreatLoanCommande {

    public Subscriber subscriber;

    public Vehicle vehicle;

    public BigDecimal amount;

    public BigDecimal interestRate;

    public Integer termMonths;

    public LocalDate startDate;

    public LocalDate endDate;

    public LoanStatus status = LoanStatus.PENDING;

    public BigDecimal monthlyPayment;

    public BigDecimal totalAmount;


}
