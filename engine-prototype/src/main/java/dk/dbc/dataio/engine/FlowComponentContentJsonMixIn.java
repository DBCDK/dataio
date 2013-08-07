package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class FlowComponentContentJsonMixIn {
    @JsonCreator
    public FlowComponentContentJsonMixIn(@JsonProperty("javascript") String javascript,
                                         @JsonProperty("invocationMethod") String invocationMethod) { }
}
