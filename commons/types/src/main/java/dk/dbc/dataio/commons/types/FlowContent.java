package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * FlowContent DTO class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowContent implements Serializable {
    private static final long serialVersionUID = 5520247158829273054L;

    private final String name;
    private final String description;
    private List<FlowComponent> components;
    private Date timeOfFlowComponentUpdate;

    /**
     * Class constructor
     *
     * @param name                      flow name
     * @param description               flow description
     * @param components                flow components attached to this flow (can be empty)
     * @param timeOfFlowComponentUpdate time of last time the flow components nested within the flow were updated
     * @throws NullPointerException     if given null-valued name, description or components argument
     * @throws IllegalArgumentException if given empty-valued name
     */
    @JsonCreator
    public FlowContent(@JsonProperty("name") String name,
                       @JsonProperty("description") String description,
                       @JsonProperty("components") List<FlowComponent> components,
                       @JsonProperty("timeOfFlowComponentUpdate") Date timeOfFlowComponentUpdate) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        // We're not making a deep-copy here, but since FlowComponent is immutable
        // (or as near as) this should be sufficient to ensure immutability of this
        // class.
        this.components = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(components, "components"));
        this.timeOfFlowComponentUpdate = timeOfFlowComponentUpdate;
    }

    public FlowContent(String name, String description, List<FlowComponent> components) {
        this(name, description, components, null);
    }

    public List<FlowComponent> getComponents() {
        return new ArrayList<>(components);
    }

    public FlowContent withComponents(FlowComponent... components) {
        final List<FlowComponent> flowComponents = new ArrayList<>();
        if (components != null && components.length > 0) {
            for (FlowComponent component : components) {
                if (component != null) {
                    flowComponents.add(component);
                }
            }
        }
        this.components = flowComponents;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public Date getTimeOfFlowComponentUpdate() {
        return timeOfFlowComponentUpdate;
    }

    public FlowContent withTimeOfFlowComponentUpdate(Date timeOfFlowComponentUpdate) {
        this.timeOfFlowComponentUpdate = new Date(timeOfFlowComponentUpdate.getTime());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowContent)) return false;

        FlowContent that = (FlowContent) o;

        return components.equals(that.components)
                && description.equals(that.description)
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + components.hashCode();
        return result;
    }
}
