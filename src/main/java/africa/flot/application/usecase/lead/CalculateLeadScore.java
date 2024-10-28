package africa.flot.application.usecase.lead;

import africa.flot.application.exceptions.LeadNotFoundException;
import africa.flot.application.ports.ScoringService;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.infrastructure.repository.LeadRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CalculateLeadScore {

    @Inject
    LeadRepository leadRepository;

    @Inject
    ScoringService scoringService;

    @WithTransaction
    public Uni<DetailedScore> execute(UUID leadId) {
        return leadRepository.findById(leadId)
                .onItem().ifNull().failWith(new LeadNotFoundException("Lead not found with id: " + leadId))
                .flatMap(scoringService::calculateScore);
    }
}
