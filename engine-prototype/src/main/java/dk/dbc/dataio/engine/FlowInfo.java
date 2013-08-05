package dk.dbc.dataio.engine;

import java.util.List;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class FlowInfo {
    private final String name;
    private final List<Component> components;

    public FlowInfo(String name, List<Component> components) {
        this.name = name;
        this.components = components;
    }

    public List<Component> getComponents() {
        return components;
    }

    public String getName() {
        return name;
    }

    @JsonCreator
    public static FlowInfo createFlowInfo(@JsonProperty("name") String name, @JsonProperty("components") List<Component> components) {
        return new FlowInfo(name, components);
    }
    
    public static class Component {
        private final int id;
        private final String javascript;
        private final String invocationMethod;
        
        public Component(int id, String javascript, String invocationMethod) {
            this.id = id;
            this.javascript = javascript;
            this.invocationMethod = invocationMethod;
        }

        public String getJavascript() {
            return javascript;
        }

        public String getInvocationMethod() {
            return invocationMethod;
        }
        
        @JsonCreator
        public static Component createComponent(@JsonProperty("id") int id,
                @JsonProperty("javascript") String javascript,
                @JsonProperty("invocationMethod") String invocationMethod) {
            return new Component(id, javascript, invocationMethod);
        }
    }
}
