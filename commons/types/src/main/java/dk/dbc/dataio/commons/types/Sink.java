package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
* Sink DTO class.
*
* In all essence objects of this class are immutable, but due to GWT serialization
* issues we cannot have final fields and need a default no-arg constructor.
*/
public class Sink implements Serializable {
    private static final long serialVersionUID = -1110221413046923805L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ SinkContent content;

    private Sink() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param id sink id (>= {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version sink version (>= {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content sink content
     *
     * @throws NullPointerException if given null-valued content
     * @throws IllegalArgumentException if value of id or version is less than lower bound
     */
    public Sink(long id, long version, SinkContent content) {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.version = InvariantUtil.checkLowerBoundOrThrow(version, "version", Constants.PERSISTENCE_VERSION_LOWER_BOUND);
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public SinkContent getContent() {
        return content;
    }
}
