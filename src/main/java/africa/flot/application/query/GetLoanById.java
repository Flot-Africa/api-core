    package africa.flot.application.query;

    import africa.flot.application.dto.query.loan.LoanDetailVM;
    import africa.flot.application.ports.LoanRepositoryPort;
    import io.smallrye.mutiny.Uni;
    import jakarta.enterprise.context.ApplicationScoped;
    import jakarta.inject.Inject;

    import java.util.UUID;

    @ApplicationScoped
    public class GetLoanById {

        @Inject
        private final LoanRepositoryPort loanRepositoryPort;

        public GetLoanById(LoanRepositoryPort loanRepositoryPort) {
            this.loanRepositoryPort = loanRepositoryPort;
        }

        public Uni<LoanDetailVM> getLoan(UUID id){
            return this.loanRepositoryPort.getLoanDetailVMById(id);
        }
    }
