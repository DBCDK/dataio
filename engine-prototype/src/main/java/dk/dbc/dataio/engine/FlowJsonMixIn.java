package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a companion to the Flow DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
abstract class FlowJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default Flow constructor.
     */
    @JsonCreator
    public FlowJsonMixIn(@JsonProperty("id") long id,
                         @JsonProperty("content") FlowContent content) { }
}
