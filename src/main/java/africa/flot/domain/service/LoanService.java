package africa.flot.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;

public interface LoanService {
    Uni<Response> getLoanProduct(Integer productId);
    Uni<Response> getClientByExternalId(String externalId);
    Uni<Response> createLoan(Integer clientId, Integer productId, BigDecimal amount);
}
