package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a companion to the FlowComponent DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class FlowComponentJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default FlowComponent constructor.
     */
    @JsonCreator
    public FlowComponentJsonMixIn(@JsonProperty("id") long id,
                                  @JsonProperty("version") long version,
                                  @JsonProperty("content") FlowComponentContent content) { }
}
