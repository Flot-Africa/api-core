package africa.flot.application.ports;

import africa.flot.application.dto.query.loan.LoanDetailVM;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface LoanRepositoryPort {
    Uni<LoanDetailVM> getLoanDetailVMById(UUID id);
}
