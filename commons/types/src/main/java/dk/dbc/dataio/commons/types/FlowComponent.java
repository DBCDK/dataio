package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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

    /**
     * Class constructor
     *
     * @param id flow component id
     * @param version flow component version
     * @param content flow component content
     *
     * @throws NullPointerException if given null-valued content argument
     */
    public FlowComponent(long id, long version, FlowComponentContent content) {
        this.id = id;
        this.version = version;
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
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