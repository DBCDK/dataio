package dk.dbc.dataio.engine;

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
    private /* final */ FlowComponentContent content;

    private FlowComponent() { }

    public FlowComponent(long id, FlowComponentContent content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public FlowComponentContent getContent() {
        return content;
    }
}