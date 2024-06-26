package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;

public class SinkBuilder {
    private int id = 42;
    private long version = 1L;
    private SinkContent content = new SinkContentBuilder().build();

    public SinkBuilder setId(int id) {
        this.id = id;
        return this;
    }

    public SinkBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public SinkBuilder setContent(SinkContent content) {
        this.content = content;
        return this;
    }

    public Sink build() {
        return new Sink(id, version, content);
    }
}
