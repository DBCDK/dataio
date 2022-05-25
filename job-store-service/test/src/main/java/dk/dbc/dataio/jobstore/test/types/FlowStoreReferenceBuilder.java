package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.FlowStoreReference;

public class FlowStoreReferenceBuilder {

    private long id = 2;
    private long version = 1;
    private String name = "name";

    public FlowStoreReferenceBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowStoreReferenceBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowStoreReferenceBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowStoreReference build() {
        return new FlowStoreReference(id, version, name);
    }

}
