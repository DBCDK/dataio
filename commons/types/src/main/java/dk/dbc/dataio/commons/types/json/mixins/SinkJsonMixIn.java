package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.SinkContent;

/**
 * This class is a companion to the Sink DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class SinkJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default Sink constructor.
     */
    @JsonCreator
    public SinkJsonMixIn(@JsonProperty("id") long id,
                         @JsonProperty("version") long version,
                         @JsonProperty("content") SinkContent content) { }
}
