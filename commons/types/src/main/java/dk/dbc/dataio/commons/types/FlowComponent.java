package dk.dbc.dataio.commons.types;

import java.io.Serializable;

/**
 * FlowComponent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class FlowComponent implements Serializable {
    private static final long serialVersionUID = 2743968388816680751L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ FlowComponentContent content;

    private FlowComponent() { }

    public FlowComponent(long id, long version, FlowComponentContent content) {
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

    public FlowComponentContent getContent() {
        return content;
    }
}