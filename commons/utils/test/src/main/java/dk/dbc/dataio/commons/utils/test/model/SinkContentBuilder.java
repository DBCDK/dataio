package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.SinkContent;

public class SinkContentBuilder {
    private String name = "name";
    private String resource = "resource";
    private String description = "description";

    public SinkContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SinkContentBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public SinkContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SinkContent build() {
        return new SinkContent(name, resource, description);
    }
}