package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

abstract class ChunkJsonMixIn {
    @JsonCreator
    public ChunkJsonMixIn(@JsonProperty("id") long id, @JsonProperty("flow") Flow flow, @JsonProperty("records") List<String> records) { }
}
