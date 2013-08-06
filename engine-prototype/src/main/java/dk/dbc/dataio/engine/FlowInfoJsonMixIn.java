package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

abstract class FlowInfoJsonMixIn {
    @JsonCreator
    public FlowInfoJsonMixIn(@JsonProperty("name") String name, @JsonProperty("components") List<FlowInfo.Component> components) { }

    public abstract static class ComponentJsonMixIn {
        @JsonCreator
        public ComponentJsonMixIn(@JsonProperty("id") int id,
                @JsonProperty("javascript") String javascript,
                @JsonProperty("invocationMethod") String invocationMethod) { }
    }
}
