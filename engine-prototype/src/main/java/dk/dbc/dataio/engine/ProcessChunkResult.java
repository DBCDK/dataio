package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import java.util.List;

class ProcessChunkResult {

    private final List<String> results;
    private final long id;
    
    ProcessChunkResult(long id, List<String> results) {
        this.id = id;
        this.results = results;
    }

    public List<String> getResults() {
        return results;
    }

    public long getId() {
        return id;
    }

    @JsonCreator
    public static ProcessChunkResult createProcessChunkResult(@JsonProperty("id") long id, @JsonProperty("records") List<String> results) {
        return new ProcessChunkResult(id, results);
    }
}
