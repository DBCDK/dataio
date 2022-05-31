package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;

public class FlowBinderBuilder {
    private long id = 62L;
    private long version = 1L;
    private FlowBinderContent content = new FlowBinderContentBuilder().build();

    public FlowBinderBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowBinderBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowBinderBuilder setContent(FlowBinderContent content) {
        this.content = content;
        return this;
    }

    public FlowBinder build() {
        return new FlowBinder(id, version, content);
    }
}
