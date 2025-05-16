package africa.flot.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateLoanCommand {
    private UUID leadId;
    private UUID vehicleId;
    private String notes;
}

