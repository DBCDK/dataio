package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;

public class SubmitterBuilder {
    private long id = 53L;
    private long version = 1L;
    private SubmitterContent content = new SubmitterContentBuilder().build();

    public SubmitterBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public SubmitterBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public SubmitterBuilder setContent(SubmitterContent content) {
        this.content = content;
        return this;
    }

    public Submitter build() {
        return new Submitter(id, version, content);
    }
}
