package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.nio.file.Path;

/**
 * This class is a companion to the Job DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
abstract class JobJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default Job constructor.
     */
    @JsonCreator
    public JobJsonMixIn(@JsonProperty("id") long id,
                        @JsonProperty("originalDataPath") Path originalDataPath,
                        @JsonProperty("flow") Flow flow) { }
}
