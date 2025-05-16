package africa.flot.application.dto.command;

import africa.flot.domain.model.enums.ReminderLevel;
import africa.flot.domain.model.enums.ReminderType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SendReminderCommand {
    private UUID loanId;
    private ReminderType type;
    private ReminderLevel level;
    private String customMessage;  // Message personnalisé optionnel
    private String createdBy = "ADMIN";
}
