package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.FlowBinderContent;

/**
 * This class is a companion to the FlowBinder DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
abstract class FlowBinderJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default FlowBinder constructor.
     */
    @JsonCreator
    public FlowBinderJsonMixIn(@JsonProperty("id") long id,
                               @JsonProperty("version") long version,
                               @JsonProperty("content") FlowBinderContent content) { }
}
