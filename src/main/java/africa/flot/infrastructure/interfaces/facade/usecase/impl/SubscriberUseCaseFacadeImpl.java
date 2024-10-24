package africa.flot.infrastructure.interfaces.facade.usecase.impl;


import africa.flot.application.command.subscriberCommande.CreerSubscriberCommande;
import africa.flot.application.command.subscriberCommande.ModifierSubscriberCommande;
import africa.flot.application.usecase.subscriber.CalculateSubscriberScore;
import africa.flot.application.usecase.subscriber.CreerSubscripteur;
import africa.flot.application.usecase.subscriber.ModifierSubscriber;
import africa.flot.domain.model.valueobject.DetailedScore;
import africa.flot.domain.model.valueobject.Score;
import africa.flot.infrastructure.interfaces.facade.usecase.SubscriberUseCaseFacade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class SubscriberUseCaseFacadeImpl implements SubscriberUseCaseFacade {

    @Inject
    CreerSubscripteur creerSubscripteur;

    @Inject
    ModifierSubscriber modifierSubscriber;

    @Inject
    CalculateSubscriberScore calculateSubscriberScore;

    @Override
    public Uni<Void> create(CreerSubscriberCommande commande) {
        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> this.creerSubscripteur.creer(commande));
    }

    @Override
    public Uni<Void> update(ModifierSubscriberCommande commande) {
        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> this.modifierSubscriber.modifier(commande));
    }

    @Override
    public Uni<DetailedScore> calculateScore(UUID subscriberId) {
        return calculateSubscriberScore.execute(subscriberId);
    }

}
