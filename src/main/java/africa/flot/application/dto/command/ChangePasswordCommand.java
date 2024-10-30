package africa.flot.application.dto.command;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangePasswordCommand {
    private String oldPassword;
    private String newPassword;

}
