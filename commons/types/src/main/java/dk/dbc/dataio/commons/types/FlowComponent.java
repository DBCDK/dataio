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
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param id flow component id (>= {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version flow component version (>= {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content flow component content
     *
     * @throws NullPointerException if given null-valued content argument
     * @throws IllegalArgumentException if value of id or version is less than lower bound
     */
    public FlowComponent(long id, long version, FlowComponentContent content) {
        this.id = InvariantUtil.checkAboveThresholdOrThrow(id, "id", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.version = InvariantUtil.checkAboveThresholdOrThrow(version, "version", Constants.PERSISTENCE_VERSION_LOWER_BOUND);
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