package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class FlowContentJsonMixIn {
    @JsonCreator
    public FlowContentJsonMixIn(@JsonProperty("name") String name,
                                @JsonProperty("description") String description,
                                @JsonProperty("components") List<FlowComponent> components) { }
}
