package africa.flot.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetailedScore {
    @JsonProperty("personalDataScore")
    private int personalDataScore;

    @JsonProperty("vtcExperienceScore")
    private int vtcExperienceScore;

    @JsonProperty("drivingRecordScore")
    private int drivingRecordScore;

    @JsonProperty("totalScore")
    private int totalScore;
}