package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Sink DTO class.
 */
public class Sink implements Serializable {
    private static final long serialVersionUID = -1110221413046923805L;

    private final long id;
    private final long version;
    private final SinkContent content;

    /**
     * Class constructor
     *
     * @param id      sink id (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version sink version (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content sink content
     * @throws NullPointerException     if given null-valued content
     * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
     */
    @JsonCreator
    public Sink(@JsonProperty("id") long id,
                @JsonProperty("version") long version,
                @JsonProperty("content") SinkContent content) {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sink)) return false;

        Sink sink = (Sink) o;

        return id == sink.id && version == sink.version
                && content.equals(sink.content);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + content.hashCode();
        return result;
    }
}
