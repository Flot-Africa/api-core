package africa.flot.application.dto.command;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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
    private Long officeId;

    private Boolean active = false;

    @JsonFormat(pattern = "dd-MMM-yyyy")
    private Integer activationDate;

    @JsonFormat(pattern = "dd-MMM-yyyy")
    private Date dateOfBirth;

    // Champs optionnels
    private String middlename;
    private Long groupId;
    private String externalId;
    private String accountNo;
    private Long staffId;
    private String mobileNo;
    private Long savingsProductId;
    private Long genderId;
    private Long clientTypeId;
    private Long clientClassificationId;
    private Long legalFormId;
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
