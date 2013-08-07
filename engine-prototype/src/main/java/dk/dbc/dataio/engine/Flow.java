package dk.dbc.dataio.engine;

import java.io.Serializable;

 /**
 * Flow DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields.
 */
public class Flow implements Serializable {
    private static final long serialVersionUID = -8809513217759455225L;

    private /* final */ long id;
    private /* final */ FlowContent content;

    public Flow(long id, FlowContent content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public FlowContent getContent() {
        return content;
    }
}
