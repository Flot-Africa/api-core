package africa.flot.application.ports;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

/**
 * Service interface for loan approval operations
 */
public interface LoanApprovalService {
    /**
     * Gets the approval template and approves the loan using template data
     */
    Uni<Response> approveLoan(String loanExternalId);
}
