package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;

public class FlowBuilder {
    private long id = 42L;
    private long version = 1L;
    private FlowContent content = new FlowContentBuilder().build();

    public FlowBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowBuilder setContent(FlowContent content) {
        this.content = content;
        return this;
    }

    public Flow build() {
        return new Flow(id, version, content);
    }
}
