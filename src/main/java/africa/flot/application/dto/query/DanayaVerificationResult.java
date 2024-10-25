package africa.flot.application.dto.query;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonObject;
import lombok.Data;
@RegisterForReflection
@Data
public class DanayaVerificationResult {
    private String id;
    private String createdAt;
    private String status;
    private PersonalInfo personalInfo;
    private VerificationScores verificationScores;

    @Data
    public static class PersonalInfo {
        private Identity identity;
        private Residence residence;
        private FamilyInfo familyInfo;
    }

    @Data
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
    public static class Residence {
        private String address;
        private String town;
    }

    @Data
    public static class FamilyInfo {
        private ParentInfo father;
        private ParentInfo mother;
        private String spouseName;
    }

    @Data
    public static class ParentInfo {
        private String firstName;
        private String lastName;
        private String birthDate;
        private String uin;
    }

    @Data
    public static class VerificationScores {
        private String expiration;
        private DBCheckScores dbCheck;
    }

    @Data
    public static class DBCheckScores {
        private Integer firstName;
        private Integer lastName;
        private Integer dateOfBirth;
        private Integer gender;
    }
}
