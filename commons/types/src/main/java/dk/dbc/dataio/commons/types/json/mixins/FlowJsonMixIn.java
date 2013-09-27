package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.FlowContent;

/**
 * This class is a companion to the Flow DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class FlowJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default Flow constructor.
     */
    @JsonCreator
    public FlowJsonMixIn(@JsonProperty("id") long id,
                         @JsonProperty("version") long version,
                         @JsonProperty("content") FlowContent content) { }
}
