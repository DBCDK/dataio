package dk.dbc.dataio.engine;

import dk.dbc.dataio.commons.types.Flow;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * This class is a companion to the Chunk DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
abstract class ChunkJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default Chunk constructor.
     */
    @JsonCreator
    public ChunkJsonMixIn(@JsonProperty("id") long id,
                          @JsonProperty("flow") Flow flow,
                          @JsonProperty("records") List<String> records) { }
}
