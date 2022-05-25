package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Submitter DTO class.
 */
public class Submitter implements Serializable {
    private static final long serialVersionUID = -2728868887371312413L;

    private final long id;
    private final long version;
    private final SubmitterContent content;

    /**
     * Class constructor
     *
     * @param id      submitter id (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version submitter version (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content submitter content
     * @throws NullPointerException     if given null-valued content argument
     * @throws IllegalArgumentException if value of id or version is less than lower bound
     */
    @JsonCreator
    public Submitter(@JsonProperty("id") long id,
                     @JsonProperty("version") long version,
                     @JsonProperty("content") SubmitterContent content) {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Submitter)) return false;

        Submitter submitter = (Submitter) o;

        return id == submitter.id
                && version == submitter.version
                && content.equals(submitter.content);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + content.hashCode();
        return result;
    }
}
