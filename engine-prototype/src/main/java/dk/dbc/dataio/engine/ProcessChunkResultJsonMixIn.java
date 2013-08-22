package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * This class is a companion to the ProcessChunkResult DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
abstract class ProcessChunkResultJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default ProcessChunkResult constructor.
     */
    @JsonCreator
    public ProcessChunkResultJsonMixIn(@JsonProperty("id") long id,
                                       @JsonProperty("results") List<String> results) { }
}
