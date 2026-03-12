package dk.dbc.dataio.commons.creatordetector.connector;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Objects;

public class CreatorNameSuggestions {

    private List<CreatorNameSuggestion> results;

    public List<CreatorNameSuggestion> getResults() {
        return results;
    }

    public void setResults(List<CreatorNameSuggestion> results) {
        this.results = results;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return results == null || results.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreatorNameSuggestions that = (CreatorNameSuggestions) o;
        return Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(results);
    }

    @Override
    public String toString() {
        return "CreatorNameSuggestions{" +
                "results=" + results +
                '}';
    }
}
