package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

@SuppressWarnings("unused")
public class FlowComponentJsonMixIn {
    @JsonCreator
    public FlowComponentJsonMixIn(@JsonProperty("id") long id,
                                  @JsonProperty("content") FlowComponentContent content) { }
}
