package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Sink;

/**
 * This class is a companion to the NewJob DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class NewJobJsonMixIn {

    /**
     * Makes jackson runtime aware of non-default NewJob constructor.
     */
    @JsonCreator
    public NewJobJsonMixIn(@JsonProperty("jobId") long jobId,
                           @JsonProperty("chunkCount") long chunkCount,
                           @JsonProperty("sink") Sink sink) {
    }
}
