package dk.dbc.dataio.commons.types.json.mixins;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a companion to the SubmitterContent DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class SubmitterContentJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default SubmitterContent constructor.
     */
    @JsonCreator
    public SubmitterContentJsonMixIn(@JsonProperty("number") long number,
                                     @JsonProperty("name") String name,
                                     @JsonProperty("description") String description) { }
}
