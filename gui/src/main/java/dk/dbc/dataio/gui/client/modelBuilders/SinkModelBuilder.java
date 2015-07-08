package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.SinkModel;

public class SinkModelBuilder {
    private long id = 64L;
    private long version = 1L;
    private String name = "name";
    private String resource = "resource";
    private String description = "description";

    public SinkModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public SinkModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public SinkModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SinkModelBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public SinkModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }


    public SinkModel build() {
        return new SinkModel(id, version, name, resource, description);
    }
}
