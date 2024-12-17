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

    // Champs obligatoires si fullname n'est pas fourni
    private String firstname;
    private String lastname;

    // Champ obligatoire pour les entreprises
    private String fullname;

    @NotNull(message = "officeId est obligatoire")
    private Integer officeId;

    private Boolean active = false;

    @JsonFormat(pattern = "dd-MMM-yyyy")
    private String activationDate;

    @JsonFormat(pattern = "dd-MMM-yyyy")
    private Date dateOfBirth;

    // Champs optionnels
    private String middlename;
    private Integer groupId;
    private Integer externalId;
    private String accountNo;
    private Integer staffId;
    private String mobileNo;
    private Integer savingsProductId;
    private Integer genderId;
    private Integer clientTypeId;
    private Integer clientClassificationId;
    private Integer legalFormId;
    private String emailAddress;


    // Configuration locale
    private String dateFormat = "dd MMMM yyyy";
    private String locale = "fr";

    // Liste d'adresses si activée
    @Valid
    private List<AddressDTO> address;

    @AssertTrue(message = "Soit firstname/lastname, soit fullname doit être fourni")
    public boolean isValidNameCombination() {
        return (firstname != null && lastname != null) || fullname != null;
    }

    @AssertTrue(message = "activationDate est requis si active est true")
    public boolean isValidActivation() {
        return !Boolean.TRUE.equals(active) || activationDate != null;
    }

}
