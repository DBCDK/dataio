package dk.dbc.dataio.commons.types;

import java.io.Serializable;
import java.util.ArrayList;
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
        // We're not making a deep-copy here, but since FlowComponent is immutable
        // (or as near as) this should be sufficient to ensure immutability of this
        // class.
        if (components != null) {
            this.components = new ArrayList<FlowComponent>(components);
        } else {
            this.components = new ArrayList<FlowComponent>(0);
        }
    }

    public List<FlowComponent> getComponents() {
        return new ArrayList<FlowComponent>(components);
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }
}
