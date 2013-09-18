package dk.dbc.dataio.engine;

import java.io.Serializable;

/**
* FlowBinder DTO class.
*
* In all essence objects of this class are immutable, but due to GWT serialization
* issues we cannot have final fields and need a default no-arg constructo.
*/
public class FlowBinder implements Serializable {
    private static final long serialVersionUID = 6196377900891717136L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ FlowBinderContent content;

    private FlowBinder() { }

    public FlowBinder(long id, long version, FlowBinderContent content) {
        this.id = id;
        this.version = version;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public FlowBinderContent getContent() {
        return content;
    }
}
