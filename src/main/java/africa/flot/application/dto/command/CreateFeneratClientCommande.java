package africa.flot.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateFeneratClientCommande {
    private Long staffId;
    private String firstname;
    private Long officeId;
    private Boolean isStaff;
    private String dateFormat;
    private Boolean active;
    private String dateOfBirth;
    private String submittedOnDate;
    private String locale;
    private List<Object> familyMembers;
    private String lastname;
    private String externalId;
    private String mobileNo;
    private String emailAddress;
    private String activationDate;
    private Long legalFormId;
}
