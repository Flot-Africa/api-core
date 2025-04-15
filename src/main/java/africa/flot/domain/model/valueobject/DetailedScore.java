package africa.flot.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class DetailedScore {
    @JsonProperty("personalDataScore")
    private double personalDataScore;

    @JsonProperty("vtcExperienceScore")
    private double vtcExperienceScore;

    @JsonProperty("drivingRecordScore")
    private double drivingRecordScore;

    @JsonProperty("totalScore")
    private double totalScore;
}