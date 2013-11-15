package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * This class is a companion to the ChunkResult DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class ChunkResultMixIn {

    /**
     * Makes jackson runtime aware of non-default ChunkResult constructor.
     */
    @JsonCreator
    public ChunkResultMixIn(@JsonProperty("jobId") long jobId,
            @JsonProperty("chunkId") long chunkId,
            @JsonProperty("encoding") String encoding,
            @JsonProperty("results") List<String> results) {
    }
}
