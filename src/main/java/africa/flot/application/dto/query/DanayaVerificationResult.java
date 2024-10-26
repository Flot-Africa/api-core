package africa.flot.application.dto.query;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.util.UUID;

@RegisterForReflection
@Data
public class DanayaVerificationResult {
    private UUID id;
    private String createdAt;
    private String status;
    private PersonalInfo personalInfo;
    private VerificationScores verificationScores;

    public boolean isValid() {
        if (verificationScores == null || verificationScores.getDbCheck() == null) {
            return false;
        }

        DBCheckScores scores = verificationScores.getDbCheck();
        return scores.getFirstName() >= 80 &&
                scores.getLastName() >= 80 &&
                scores.getDateOfBirth() >= 80 &&
                scores.getGender() >= 80;
    }

    @Data
    @RegisterForReflection
    public static class PersonalInfo {
        private Identity identity;
        private Residence residence;
        private FamilyInfo familyInfo;
    }

    @Data
    @RegisterForReflection
    public static class Identity {
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String documentExpiry;
        private String nni;
        private String gender;
        private String placeOfBirth;
        private String nationality;
        private String documentNumber;
    }

    @Data
    @RegisterForReflection
    public static class Residence {
        private String address;
        private String town;
    }

    @Data
    @RegisterForReflection
    public static class FamilyInfo {
        private ParentInfo father;
        private ParentInfo mother;
        private String spouseName;
    }

    @Data
    @RegisterForReflection
    public static class ParentInfo {
        private String firstName;
        private String lastName;
        private String birthDate;
        private String uin;
    }

    @Data
    @RegisterForReflection
    public static class VerificationScores {
        private String expiration;
        private DBCheckScores dbCheck;
    }

    @Data
    @RegisterForReflection
    public static class DBCheckScores {
        private Integer firstName;
        private Integer lastName;
        private Integer dateOfBirth;
        private Integer gender;
    }
}