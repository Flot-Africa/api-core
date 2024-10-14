package africa.flot.application.ports;

import africa.flot.domain.model.Subscriber;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface SubscripteurRepositoryPorts {

   Uni<Void> enregistrer(Subscriber subscriber);

   Uni<Subscriber> recupererParId(UUID id);

   Uni<Boolean> existeParId(UUID id);

    Uni<Void> updates(Subscriber subscriber);
}
