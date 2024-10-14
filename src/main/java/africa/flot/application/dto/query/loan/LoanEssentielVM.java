package africa.flot.application.dto.query.loan;

import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.Vehicle;
import africa.flot.domain.model.enums.LoanStatus;

import java.time.LocalDate;


public class LoanEssentielVM {

    public Subscriber subscriber;

    public Vehicle vehicle;

    public Double amount;

    public Double interestRate;

    public Integer termMonths;

    public LocalDate startDate;

    public LocalDate endDate;

    public LoanStatus status = LoanStatus.PENDING;

    public Double monthlyPayment;

    public Double totalAmount;



    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public Double getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(Double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }


}
