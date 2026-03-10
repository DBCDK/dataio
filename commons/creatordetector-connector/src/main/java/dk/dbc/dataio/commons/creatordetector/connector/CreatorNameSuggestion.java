package dk.dbc.dataio.commons.creatordetector.connector;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.Objects;

public class CreatorNameSuggestion {
    private final String authorityId;
    private final String name;
    private final double matchScore;
    private final double rerankScore;

    @JsonCreator
    public CreatorNameSuggestion(List<Object> values) {
        this.authorityId = (String) values.get(0);
        this.name = (String) values.get(1);
        this.matchScore = ((Number) values.get(2)).doubleValue();
        this.rerankScore = ((Number) values.get(3)).doubleValue();
    }

    public String getAuthority() {
        return authorityId;
    }
    public String getName() {
        return name;
    }
    public double getMatchScore() {
        return matchScore;
    }
    public double getRerankScore() {
        return rerankScore;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreatorNameSuggestion that = (CreatorNameSuggestion) o;
        return Double.compare(matchScore, that.matchScore) == 0 && Double.compare(rerankScore, that.rerankScore) == 0 && Objects.equals(authorityId, that.authorityId) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorityId, name, matchScore, rerankScore);
    }

    @Override
    public String toString() {
        return "CreatorNameSuggestion{" +
                "authorityId='" + authorityId + '\'' +
                ", name='" + name + '\'' +
                ", matchScore=" + matchScore +
                ", rerankScore=" + rerankScore +
                '}';
    }
}
