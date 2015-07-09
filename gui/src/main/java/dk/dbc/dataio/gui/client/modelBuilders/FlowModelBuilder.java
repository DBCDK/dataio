package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowModelBuilder {
    private long id = 3L;
    private long version = 1L;
    private String name = "name";
    private String description = "description";
    private List<FlowComponentModel> flowComponents = new ArrayList<FlowComponentModel>(Collections.singletonList(
            new FlowComponentModelBuilder().build()));

    public FlowModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowModelBuilder setComponents(List<FlowComponentModel> flowComponents) {
        this.flowComponents = new ArrayList<FlowComponentModel>(flowComponents);
        return this;
    }

    public FlowModel build() {
        return new FlowModel(id, version, name, description, flowComponents);
    }
}
