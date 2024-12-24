package africa.flot.domain.model.valueobject;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;


@Data
public class ProfessionalInfo implements Serializable {
    private boolean isFrench;

    private String profession;

    private String jobSituation;

    private String socialProCategory;

    private String place;

    private String risk;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfessionalInfo that = (ProfessionalInfo) o;
        return isFrench == that.isFrench && Objects.equals(profession, that.profession) && Objects.equals(jobSituation, that.jobSituation) && Objects.equals(socialProCategory, that.socialProCategory) && Objects.equals(place, that.place) && Objects.equals(risk, that.risk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFrench, profession, jobSituation, socialProCategory, place, risk);
    }

    @Override
    public String toString() {
        return "ProfessionalInfo{" +
                "isFrench=" + isFrench +
                ", profession='" + profession + '\'' +
                ", jobSituation='" + jobSituation + '\'' +
                ", socialProCategory='" + socialProCategory + '\'' +
                ", place='" + place + '\'' +
                ", risk='" + risk + '\'' +
                '}';
    }
}