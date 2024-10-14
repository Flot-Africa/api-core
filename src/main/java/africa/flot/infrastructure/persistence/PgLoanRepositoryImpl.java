package africa.flot.infrastructure.persistence;

import africa.flot.application.dto.query.loan.LoanDetailVM;
import africa.flot.application.ports.LoanRepositoryPort;
import africa.flot.domain.model.Loan;
import africa.flot.infrastructure.mapper.LoanMappers;
import africa.flot.infrastructure.repository.LoanRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class PgLoanRepositoryImpl implements LoanRepositoryPort, LoanRepository {

    @Inject
    LoanMappers loanMappers;
    @Override
    public Uni<LoanDetailVM> getLoanDetailVMById(UUID id) {

        return Loan.findById(id)
                .onItem().ifNotNull()
                .transform(loan -> loanMappers.loanToLonnDetailVM((Loan) loan))
                .onItem().ifNull().failWith(new RuntimeException("Loan not found"));
    }

}
