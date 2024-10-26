package africa.flot.application.usecase.subscriber;


import africa.flot.application.command.subscriberCommande.ModifierSubscriberCommande;
import africa.flot.application.exceptions.SubscriberNotFondExeption;
import africa.flot.application.ports.SubscripteurRepositoryPorts;
import africa.flot.domain.model.enums.SubscriberStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class ModifierSubscriber {

    @Inject
    SubscripteurRepositoryPorts subscripteurRepositoryPorts;

    @Inject
    public ModifierSubscriber(SubscripteurRepositoryPorts subscripteurRepositoryPorts) {
        this.subscripteurRepositoryPorts = subscripteurRepositoryPorts;
    }

    public void modifier(ModifierSubscriberCommande commande) {
        UUID id = commande.getId();
        this.subscripteurRepositoryPorts.recupererParId(id)
                .onItem().ifNull().failWith(() -> new SubscriberNotFondExeption("Aucun subscripteur trouvé pour modification!"))
                .flatMap(subscriber -> {
                    // Mettre à jour les champs du subscriber avec les valeurs de la commande
                    subscriber.setTelephone(commande.getTelephone());
                    subscriber.setNom(commande.getNom());
                    subscriber.setPrenom(commande.getPrenom());
                    subscriber.setDateNaissance(commande.getDateNaissance());
                    subscriber.setDateObtentionPermis(commande.getDateObtentionPermis());
                    subscriber.setDateDebutExerciceVTC(commande.getDateDebutExerciceVTC());
                    subscriber.setRevenuMensuel(commande.getRevenuMensuel());
                    subscriber.setChargesMensuelles(commande.getChargesMensuelles());
                    subscriber.setMaritalStatus(commande.getSituationMatrimoniale());
                    subscriber.setHabitation(commande.getHabitation());
                    subscriber.setRevenuesConjoint(commande.getRevenuesConjoint());
                    subscriber.setNombreEnfants(commande.getNombreEnfants());
                    subscriber.setStatus(SubscriberStatus.LEADS);

                    return this.subscripteurRepositoryPorts.updates(subscriber);
                })
                .subscribe().with(
                        success -> System.out.println("Modification réussie pour le subscriber avec l'ID : " + id),
                        failure -> System.err.println("Erreur lors de la modification : " + failure.getMessage())
                );
    }

}
