package africa.flot.infrastructure.interfaces.facade.usecase.impl;


import africa.flot.application.command.subscriberCommande.CreerSubscriberCommande;
import africa.flot.application.command.subscriberCommande.ModifierSubscriberCommande;
import africa.flot.application.usecase.subscriber.CreerSubscripteur;
import africa.flot.application.usecase.subscriber.ModifierSubscriber;
import africa.flot.infrastructure.interfaces.facade.usecase.SubscriberUseCaseFacade;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SubscriberUseCaseFacadeImpl implements SubscriberUseCaseFacade {

    @Inject
    CreerSubscripteur creerSubscripteur;

    @Inject
    ModifierSubscriber modifierSubscriber;

    @Override
    public Uni<Void> creer(CreerSubscriberCommande commande) {
        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> this.creerSubscripteur.creer(commande));
    }

    @Override
    public Uni<Void> modifier(ModifierSubscriberCommande commande) {
        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> this.modifierSubscriber.modifier(commande));
    }



}
