package africa.flot.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DetailedScore {
    @JsonProperty("personalDataScore")
    private final int personalDataScore;

    @JsonProperty("evCostScore")
    private final int evCostScore;

    @JsonProperty("incomeScore")
    private final int incomeScore;

    @JsonProperty("vtcExperienceScore")
    private final int vtcExperienceScore;

    @JsonProperty("drivingRecordScore")
    private final int drivingRecordScore;

    @JsonProperty("totalScore")
    private final Score totalScore;

    public DetailedScore(int personalDataScore, int evCostScore, int incomeScore,
                         int vtcExperienceScore, int drivingRecordScore, int totalScoreValue) {
        this.personalDataScore = personalDataScore;
        this.evCostScore = evCostScore;
        this.incomeScore = incomeScore;
        this.vtcExperienceScore = vtcExperienceScore;
        this.drivingRecordScore = drivingRecordScore;
        this.totalScore = new Score(totalScoreValue);
    }

    // Getters...
}
