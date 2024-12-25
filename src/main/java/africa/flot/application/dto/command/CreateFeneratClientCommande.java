package africa.flot.application.dto.command;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateFeneratClientCommande {
    private long staffId;
    private String firstname;
    private long officeId;
    private boolean isStaff;
    private String dateFormat;
    private boolean active;
    private String dateOfBirth;
    private String submittedOnDate;
    private String locale;
    private List<Object> familyMembers;
    private String lastname;
    private String externalId;
    private String mobileNo;
    private String emailAddress;
    private String activationDate;


}
