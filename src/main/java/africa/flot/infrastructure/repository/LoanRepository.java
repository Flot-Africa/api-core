package africa.flot.infrastructure.repository;


import africa.flot.domain.model.Loan;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;


public interface LoanRepository extends PanacheRepositoryBase<Loan, UUID> {

   /* public Uni<List<Loan>> findBySubscriberId(UUID subscriberId) {
        return list("subscriber.id", subscriberId);
    }*/
}
