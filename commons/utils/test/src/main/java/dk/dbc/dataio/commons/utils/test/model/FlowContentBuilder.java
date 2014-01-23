package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowContentBuilder {
    private String name = "name";
    private String description = "description";
    private List<FlowComponent> components = new ArrayList<>(Arrays.asList(
            new FlowComponentBuilder().build()));

    public FlowContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowContentBuilder setComponents(List<FlowComponent> components) {
        this.components = new ArrayList<>(components);
        return this;
    }

    public FlowContent build() {
        return new FlowContent(name, description, components);
    }
}
