package africa.flot.infrastructure.interfaces.facade.query;

import africa.flot.application.dto.query.loan.LoanDetailVM;
import io.smallrye.mutiny.Uni;

import java.util.UUID;


public interface LoanQueryFacade {
    Uni<LoanDetailVM> findById(UUID id);
}
