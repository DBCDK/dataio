package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowContent DTO class.
 */
public class FlowContent implements Serializable {
    private static final long serialVersionUID = 5520247158829273054L;

    private final String name;
    private final String description;
    private final List<FlowComponent> components;

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
    @JsonCreator
    public FlowContent(@JsonProperty("name") String name,
                       @JsonProperty("description") String description,
                       @JsonProperty("components") List<FlowComponent> components) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        // We're not making a deep-copy here, but since FlowComponent is immutable
        // (or as near as) this should be sufficient to ensure immutability of this
        // class.
        this.components = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(components, "components"));
    }

    public List<FlowComponent> getComponents() {
        return new ArrayList<>(components);
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowContent)) return false;

        FlowContent that = (FlowContent) o;

        if (!components.equals(that.components)) return false;
        if (!description.equals(that.description)) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + components.hashCode();
        return result;
    }
}
