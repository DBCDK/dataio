package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.ChunkItem;

import java.util.List;

/**
 * This class is a companion to the SinkChunkResult DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class SinkChunkResultMixIn {

    /**
     * Makes jackson runtime aware of non-default ChunkResult constructor.
     */
    @JsonCreator
    public SinkChunkResultMixIn(@JsonProperty("jobId") long jobId,
            @JsonProperty("chunkId") long chunkId,
            @JsonProperty("encoding") String encoding,
            @JsonProperty("items") List<ChunkItem> items) {
    }
}
