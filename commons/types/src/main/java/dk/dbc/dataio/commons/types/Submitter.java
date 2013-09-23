package dk.dbc.dataio.commons.types;

import java.io.Serializable;

/**
* Submitter DTO class.
*
* In all essence objects of this class are immutable, but due to GWT serialization
* issues we cannot have final fields and need a default no-arg constructo.
*/
public class Submitter implements Serializable {
    private static final long serialVersionUID = -2728868887371312413L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ SubmitterContent content;

    private Submitter() { }

    public Submitter(long id, long version, SubmitterContent content) {
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

    public SubmitterContent getContent() {
        return content;
    }
}
