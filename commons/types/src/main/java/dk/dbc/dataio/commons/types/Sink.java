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
    static /* final */ long ID_VERSION_LOWER_THRESHOLD = 0;

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
     * @param id sink id (> {@value #ID_VERSION_LOWER_THRESHOLD})
     * @param version sink version (> {@value #ID_VERSION_LOWER_THRESHOLD})
     * @param content sink content
     *
     * @throws NullPointerException if given null-valued content
     * @throws IllegalArgumentException if value of id or version is not above {@value #ID_VERSION_LOWER_THRESHOLD}
     */
    public Sink(long id, long version, SinkContent content) {
        this.id = InvariantUtil.checkAboveThresholdOrThrow(id, "id", ID_VERSION_LOWER_THRESHOLD);
        this.version = InvariantUtil.checkAboveThresholdOrThrow(version, "version", ID_VERSION_LOWER_THRESHOLD);
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
