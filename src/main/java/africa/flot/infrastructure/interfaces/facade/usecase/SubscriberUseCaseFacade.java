package africa.flot.infrastructure.interfaces.facade.usecase;

import africa.flot.application.command.subscriberCommande.CreerSubscriberCommande;
import africa.flot.application.command.subscriberCommande.ModifierSubscriberCommande;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.domain.model.valueobject.Score;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface SubscriberUseCaseFacade {

    Uni<Void> create(CreerSubscriberCommande commande);
    Uni<Void> update(ModifierSubscriberCommande commande);

    Uni<DetailedScore> calculateScore(UUID subscriberId);

}
