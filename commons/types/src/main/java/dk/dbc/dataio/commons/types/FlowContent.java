package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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

    /**
     * Class constructor
     *
     * @param name flow name
     * @param description flow description
     * @param components flow components attached to this flow (can be empty)
     *
     * @throws NullPointerException if given null-valued name, description or components argument
     * @throws IllegalArgumentException if given empty-valued name or description argument
     */
    public FlowContent(String name, String description, List<FlowComponent> components) {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        // We're not making a deep-copy here, but since FlowComponent is immutable
        // (or as near as) this should be sufficient to ensure immutability of this
        // class.
        this.components = new ArrayList<FlowComponent>(InvariantUtil.checkNotNullOrThrow(components, "components"));
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
