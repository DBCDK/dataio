package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

@SuppressWarnings("unused")
abstract class FlowJsonMixIn {
    @JsonCreator
    public FlowJsonMixIn(@JsonProperty("id") long id,
                         @JsonProperty("content") FlowContent content) { }
}
