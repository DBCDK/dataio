package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;

public class FlowComponentBuilder {
    private Long id = 42L;
    private Long version = 1L;
    private FlowComponentContent content = new FlowComponentContentBuilder().build();

    public FlowComponentBuilder setId(Long id) {
        this.id = id;
        return this;
    }

    public FlowComponentBuilder setVersion(Long version) {
        this.version = version;
        return this;
    }

    public void setContent(FlowComponentContent content) {
        this.content = content;
    }

    public FlowComponent build() {
        return new FlowComponent(id, version, content);
    }
}
