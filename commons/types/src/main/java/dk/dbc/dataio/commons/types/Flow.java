package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Flow DTO class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Flow implements Serializable {
    private static final long serialVersionUID = -8809513217759455225L;

    private final long id;
    private final long version;
    private final FlowContent content;

    /**
     * Class constructor
     *
     * @param id      flow id (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version flow version (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content flow content
     * @throws NullPointerException     if given null-valued content
     * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
     */
    @JsonCreator
    public Flow(@JsonProperty("id") long id,
                @JsonProperty("version") long version,
                @JsonProperty("content") FlowContent content) {

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

    public FlowContent getContent() {
        return content;
    }

    @JsonIgnore
    public boolean hasNextComponents() {
        for (FlowComponent flowComponent : content.getComponents()) {
            if (flowComponent.getNext() != FlowComponent.UNDEFINED_NEXT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flow)) return false;

        Flow flow = (Flow) o;

        return id == flow.id
                && version == flow.version
                && content.equals(flow.content);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + content.hashCode();
        return result;
    }
}
