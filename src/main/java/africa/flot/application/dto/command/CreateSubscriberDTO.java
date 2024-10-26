package africa.flot.application.dto.command;

import africa.flot.application.dto.query.AddressDTO;
import africa.flot.domain.model.enums.MaritalStatus;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CreateSubscriberDTO {

    @NotBlank(message = "Le téléphone est obligatoire.")
    public String phone;

    @NotBlank(message = "Le prénom est obligatoire.")
    public String firstName;

    @NotBlank(message = "Le nom est obligatoire.")
    public String lastName;

    @Past(message = "La date de naissance doit être dans le passé.")
    public LocalDate dateOfBirth;

    @Email(message = "L'email doit être valide.")
    @NotBlank(message = "L'email est obligatoire.")
    public String email;



   /* @NotBlank(message = "Le mot de passe est obligatoire.")
    public String password;*/



    @NotNull(message = "L'adresse est obligatoire.")
    public AddressDTO address;

    @PastOrPresent(message = "La date d'obtention du permis doit être dans le passé ou le présent.")
    public LocalDate driverLicenseDate;

    @PastOrPresent(message = "La date de début d'exercice VTC doit être dans le passé ou le présent.")
    public LocalDate vtcStartDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le revenu mensuel doit être positif.")
    public Double monthlyIncome;

    @DecimalMin(value = "0.0", inclusive = false, message = "Les charges mensuelles doivent être positives.")
    public Double monthlyCharges;

    @NotNull(message = "La situation matrimoniale est obligatoire.")
    public MaritalStatus maritalStatus;

    @Min(value = 0, message = "Le nombre d'enfants ne peut pas être négatif.")
    public Integer numberOfChildren;

    @NotBlank(message = "Le numéro de CNI est obligatoire.")
    public String driverLicenseNumber;

    public String siretNumber;
    public String rcsNumber;
    public String vatNumber;

    @NotBlank(message = "La nationalité est obligatoire.")
    public String nationality;

    @NotBlank(message = "Le pays de naissance est obligatoire.")
    public String countryOfBirth;

    @NotBlank(message = "La ville de naissance est obligatoire.")
    public String cityOfBirth;
}
