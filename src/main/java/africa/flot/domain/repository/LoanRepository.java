package africa.flot.domain.repository;


import africa.flot.domain.model.Loan;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LoanRepository implements PanacheRepositoryBase<Loan, UUID> {

    // Additional query methods

    public Uni<List<Loan>> findBySubscriberId(UUID subscriberId) {
        return list("subscriber.id", subscriberId);
    }
}
