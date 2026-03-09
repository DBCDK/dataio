package dk.dbc.dataio.commons.creatordetector.connector;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CreatorNameSuggestions {

    // Provided the Jackson JSON parser reads the object fields in the order they appear in the JSON
    // response, the results map will be in the same order as the JSON response bacause of the LinkedHashMap.
    @JsonProperty("results")
    private LinkedHashMap<String, List<CreatorNameSuggestion>> results;

    public Map<String, List<CreatorNameSuggestion>> getResults() {
        return results;
    }

    public void setResults(LinkedHashMap<String, List<CreatorNameSuggestion>> results) {
        this.results = results;
    }

    /**
     * Convenience view of {@link #getResults()} returning only the first suggestion per key.
     * Keys with null/empty suggestion lists are omitted.
     */
    public Map<String, CreatorNameSuggestion> getTopResults() {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, CreatorNameSuggestion> first = new LinkedHashMap<>();
        for (Map.Entry<String, List<CreatorNameSuggestion>> e : results.entrySet()) {
            List<CreatorNameSuggestion> suggestions = e.getValue();
            if (suggestions != null && !suggestions.isEmpty() && suggestions.get(0) != null) {
                first.put(e.getKey(), suggestions.get(0));
            }
        }
        return first;
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
