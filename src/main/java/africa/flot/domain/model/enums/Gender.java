package africa.flot.domain.model.enums;

public enum Gender {
    MASCULIN,
    FEMININ,
    MALE,
    FEMALE,
    NON_DEFINI;

    public static Gender fromDanayaGender(String danayaGender) {
        if (danayaGender == null) return NON_DEFINI;

        switch (danayaGender.toUpperCase()) {
            case "M":
            case "MALE":
            case "MASCULIN":
                return MASCULIN;
            case "F":
            case "FEMALE":
            case "FEMININ":
                return FEMININ;
            default:
                return NON_DEFINI;
        }
    }
}
