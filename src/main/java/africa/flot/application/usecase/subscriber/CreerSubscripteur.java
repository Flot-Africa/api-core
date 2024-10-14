package africa.flot.application.usecase.subscriber;


import africa.flot.application.command.subscriberCommande.CreerSubscriberCommande;
import africa.flot.application.ports.SubscripteurRepositoryPorts;
import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.enums.SubscriberStatus;
import africa.flot.domain.model.valueobject.Address;
import africa.flot.presentation.util.ApiResponseBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;


@ApplicationScoped
public class CreerSubscripteur {

    private static final Logger LOG = Logger.getLogger(ApiResponseBuilder.class);

    @Inject
    private final SubscripteurRepositoryPorts subscripteurRepositoryPorts;


    @Inject
    public CreerSubscripteur(SubscripteurRepositoryPorts subscripteurRepositoryPorts) {
        this.subscripteurRepositoryPorts = subscripteurRepositoryPorts;
    }

    public void creer(CreerSubscriberCommande commande){
        Subscriber subscriber =  this.generate(commande);
        this.subscripteurRepositoryPorts.enregistrer(subscriber).
                onFailure().invoke(e -> LOG.error("Erreur lors de l'enregistrement du subscriber", e))
                .subscribe().with(
                        success -> LOG.info("Subscriber enregistré"),
                        failure -> LOG.error("Échec lors de l'enregistrement")
                );

    }


    /*private void subscriberStatus(SubscriberStatus status) {
        boolean status = this.subscripteurRepositoryPorts.findSubscriberStatus(status);
    }*/


    private Subscriber generate(CreerSubscriberCommande commande) {

        Address address = new Address();
        address.setZoneResidence(commande.getAdresseCommande().getZoneResidence());
        address.setPaysNaissance(commande.getAdresseCommande().getPaysNaissance());
        address.setVilleNaissance(commande.getAdresseCommande().getVilleNaissance());

        var subscriber = new Subscriber();
        subscriber.setId(UUID.randomUUID());
        subscriber.setTelephone(commande.getTelephone());
        subscriber.setNom(commande.getNom());
        subscriber.setPrenom(commande.getPrenom());
        subscriber.setDateNaissance(commande.getDateNaissance());
        subscriber.setAddress(address);
        subscriber.setDateObtentionPermis(commande.getDateObtentionPermis());
        subscriber.setDateDebutExerciceVTC(commande.getDateDebutExerciceVTC());
        subscriber.setRevenuMensuel(commande.getRevenuMensuel());
        subscriber.setChargesMensuelles(commande.getChargesMensuelles());
        subscriber.setSituationMatrimoniale(commande.getSituationMatrimoniale());
        subscriber.setHabitation(commande.getHabitation());
        subscriber.setRevenuesConjoint(commande.getRevenuesConjoint());
        subscriber.setNombreEnfants(commande.getNombreEnfants());
        subscriber.setStatus(SubscriberStatus.LEADS);
        return subscriber;
    }


}

