package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Flow;

import java.util.List;

/**
 * This class is a companion to the Chunk DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class ChunkMixIn {

    /**
     * Makes jackson runtime aware of non-default Chunk constructor.
     */
    @JsonCreator
    public ChunkMixIn(@JsonProperty("jobId") long jobId,
                      @JsonProperty("chunkId") long chunkId,
                      @JsonProperty("flow") Flow flow,
                      @JsonProperty("records") List<String> records) {
    }
}
