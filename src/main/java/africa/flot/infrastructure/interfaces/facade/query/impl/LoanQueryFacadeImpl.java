package africa.flot.infrastructure.interfaces.facade.query.impl;

import africa.flot.application.dto.query.loan.LoanDetailVM;
import africa.flot.application.query.GetLoanById;
import africa.flot.infrastructure.interfaces.facade.query.LoanQueryFacade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class LoanQueryFacadeImpl implements LoanQueryFacade {

    @Inject
    GetLoanById getLoanById;

    @Override
    public Uni<LoanDetailVM> findById(UUID id) {
        return this.getLoanById.getLoan(id)
                .onItem().transform(loanDetailVM -> loanDetailVM);
    }
}
