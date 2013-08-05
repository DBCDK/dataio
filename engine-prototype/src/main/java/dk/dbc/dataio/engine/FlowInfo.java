package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class FlowInfo {
    private final String name;

    public FlowInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @JsonCreator
    public static FlowInfo createFlowInfo(@JsonProperty("name") String name) {
        return new FlowInfo(name);
    }
}
