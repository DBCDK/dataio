package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * FlowBinder DTO class.
 */
public class FlowBinder implements Serializable {
    private static final long serialVersionUID = 6196377900891717136L;

    private final long id;
    private final long version;
    private final FlowBinderContent content;

    /**
     * Class constructor
     *
     * @param id      flow binder id (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version flow binder version (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content flow binder content
     * @throws NullPointerException     if given null-valued content
     * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
     */

    @JsonCreator
    public FlowBinder(@JsonProperty("id") long id,
                      @JsonProperty("version") long version,
                      @JsonProperty("content") FlowBinderContent content) {

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

    public FlowBinderContent getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowBinder)) return false;

        FlowBinder that = (FlowBinder) o;

        return id == that.id
                && version == that.version
                && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + content.hashCode();
        return result;
    }
}
