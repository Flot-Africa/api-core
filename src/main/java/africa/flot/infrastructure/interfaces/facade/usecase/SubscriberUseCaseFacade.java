package africa.flot.infrastructure.interfaces.facade.usecase;

import africa.flot.application.command.subscriberCommande.CreerSubscriberCommande;
import africa.flot.application.command.subscriberCommande.ModifierSubscriberCommande;
import io.smallrye.mutiny.Uni;

public interface SubscriberUseCaseFacade {

    Uni<Void> creer(CreerSubscriberCommande commande);
    Uni<Void> modifier(ModifierSubscriberCommande commande);


}
