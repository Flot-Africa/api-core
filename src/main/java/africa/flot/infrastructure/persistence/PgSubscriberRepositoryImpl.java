package africa.flot.infrastructure.persistence;

import africa.flot.application.ports.SubscripteurRepositoryPorts;
import africa.flot.domain.model.Subscriber;
import africa.flot.infrastructure.repository.SubscripteurRepository;
import africa.flot.presentation.util.ApiResponseBuilder;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.UUID;


@ApplicationScoped
public class PgSubscriberRepositoryImpl implements SubscripteurRepositoryPorts, SubscripteurRepository {

    private static final Logger LOG = Logger.getLogger(ApiResponseBuilder.class);


    @WithTransaction
    @Override
    public Uni<Void> enregistrer(Subscriber subscriber) {
        return Subscriber.persist(subscriber)
                .onItem().invoke(() -> {
                    LOG.info("Subscriber enregistré avec succès dans la base de données");
                })
                .onFailure().invoke(e -> LOG.error("Erreur lors de l'enregistrement", e))
                .replaceWithVoid();
    }


    @WithTransaction
    @WithSession
    @Override
    public Uni<Void> updates(Subscriber subscriber) {
        return Subscriber.update(
                requete,
                subscriber.getTelephone(),
                subscriber.getNom(),
                subscriber.getPrenom(),
                subscriber.getDateNaissance(),
                subscriber.getDateObtentionPermis(),
                subscriber.getDateDebutExerciceVTC(),
                subscriber.getRevenuMensuel(),
                subscriber.getChargesMensuelles(),
                subscriber.getSituationMatrimoniale(),
                subscriber.getHabitation(),
                subscriber.getRevenuesConjoint(),
                subscriber.getNombreEnfants(),
                subscriber.getStatus(),
                subscriber.getId()
        ).replaceWithVoid();
    }



    @WithTransaction
    @Override
    public Uni<Subscriber> recupererParId(UUID id) {
        return Subscriber.findById(id);
    }

    @WithTransaction
    @Override
    public Uni<Boolean> existeParId(UUID id) {
        return Subscriber.count("id", id)
                .map(count -> count > 0);
    }



    String requete = "telephone = ?1, nom = ?2, prenom = ?3, dateNaissance = ?4, dateObtentionPermis = ?5, " +
            "dateDebutExerciceVTC = ?6, revenuMensuel = ?7, chargesMensuelles = ?8, situationMatrimoniale = ?9, " +
            "habitation = ?10, revenuesConjoint = ?11, nombreEnfants = ?12, status = ?13 " +
            "where id = ?14";


}
