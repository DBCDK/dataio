package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

abstract class ProcessChunkResultJsonMixIn {
    @JsonCreator
    public ProcessChunkResultJsonMixIn(@JsonProperty("id") long id, @JsonProperty("records") List<String> results) { }
}
