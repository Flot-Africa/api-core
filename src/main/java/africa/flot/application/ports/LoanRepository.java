package africa.flot.application.ports;

import africa.flot.domain.model.Loan;

import java.util.Optional;

public interface LoanRepository {
    String save(Loan loan);
    Optional<Loan> findById(String id);
}