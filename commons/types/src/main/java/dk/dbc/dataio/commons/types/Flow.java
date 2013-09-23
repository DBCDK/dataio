package dk.dbc.dataio.commons.types;

import java.io.Serializable;

 /**
 * Flow DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructo.
 */
public class Flow implements Serializable {
    private static final long serialVersionUID = -8809513217759455225L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ FlowContent content;

    private Flow() { }

    public Flow(long id, long version, FlowContent content) {
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

     public FlowContent getContent() {
        return content;
    }
}
