package africa.flot.domain.model.valueobject;

import africa.flot.domain.model.enums.ScoreCategory;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Score {
    private final int value;
    private final ScoreCategory category;

    @JsonCreator
    public Score(@JsonProperty("value") int value) {
        this.value = value;
        this.category = calculateCategory(value);
    }

    @JsonProperty("value")
    public int getValue() {
        return value;
    }

    @JsonProperty("category")
    public ScoreCategory getCategory() {
        return category;
    }

    private ScoreCategory calculateCategory(int value) {
        if (value < 300) return ScoreCategory.LOW;
        if (value < 600) return ScoreCategory.MEDIUM;
        return ScoreCategory.HIGH;
    }

    @Override
    public String toString() {
        return "Score{value=" + value + ", category=" + category + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score = (Score) o;
        return value == score.value && category == score.category;
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(value) + (category != null ? category.hashCode() : 0);
    }
}

