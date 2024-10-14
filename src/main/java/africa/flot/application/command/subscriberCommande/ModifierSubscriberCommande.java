package africa.flot.application.command.subscriberCommande;

import java.util.UUID;

public class ModifierSubscriberCommande extends CreerSubscriberCommande {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
