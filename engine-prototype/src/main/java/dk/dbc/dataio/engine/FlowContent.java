package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.util.List;

/**
 * FlowContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class FlowContent implements Serializable {
    private static final long serialVersionUID = 5520247158829273054L;

    private /* final */ String name;
    private /* final */ String description;
    private /* final */ List<FlowComponent> components;

    private FlowContent() { }

    public FlowContent(String name, String description, List<FlowComponent> components) {
        this.name = name;
        this.description = description;
        this.components = components;
    }

    public List<FlowComponent> getComponents() {
        return components;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }
}
