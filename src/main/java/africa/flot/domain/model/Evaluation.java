package africa.flot.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class Evaluation extends PanacheEntityBase {
    @Id
    @GeneratedValue
    public UUID id;

    public boolean inscriptionValidee;
    public boolean contratVTC;
    public boolean evaluationVTC;
    public String scoreGlobalEvaluation;
    public String commentaireEvaluation;
}
