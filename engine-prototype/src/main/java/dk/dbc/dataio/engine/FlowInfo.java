package dk.dbc.dataio.engine;

import java.util.List;

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
    }
}
