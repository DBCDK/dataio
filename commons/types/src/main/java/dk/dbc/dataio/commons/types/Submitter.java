package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
* Submitter DTO class.
*
* In all essence objects of this class are immutable, but due to GWT serialization
* issues we cannot have final fields and need a default no-arg constructor.
*/
public class Submitter implements Serializable {
    private static final long serialVersionUID = -2728868887371312413L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ SubmitterContent content;

    private Submitter() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param id submitter id (>= {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version submitter version (>= {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content submitter content
     *
     * @throws NullPointerException if given null-valued content argument
     * @throws IllegalArgumentException if value of id or version is less than lower bound
     */
    public Submitter(long id, long version, SubmitterContent content) {
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

    public SubmitterContent getContent() {
        return content;
    }
}
