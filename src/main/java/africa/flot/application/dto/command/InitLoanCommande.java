package africa.flot.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InitLoanCommande {
    private UUID leadId;
    private Integer produitId;
    private UUID vehicleId; // Ajout de l'ID du véhicule
}