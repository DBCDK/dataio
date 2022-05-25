package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
public abstract class HarvesterConfig<T> implements Serializable {
    private static final long serialVersionUID = 4610025048980946641L;

    private /* final */ long id;
    private /* final */ long version;
    private /* final */ T content;

    @JsonCreator
    public HarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") T content)
            throws NullPointerException, IllegalArgumentException {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.version = InvariantUtil.checkLowerBoundOrThrow(version, "version", Constants.PERSISTENCE_VERSION_LOWER_BOUND);
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
    }

    protected HarvesterConfig() {
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public T getContent() {
        return content;
    }

    @JsonIgnore
    public String getType() {
        return this.getClass().getName();
    }

    @JsonIgnore
    public abstract String getLogId();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HarvesterConfig<?> that = (HarvesterConfig<?>) o;

        if (id != that.id) {
            return false;
        }
        if (version != that.version) {
            return false;
        }
        return content != null ? content.equals(that.content) : that.content == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HarvesterConfig{");
        sb.append("id=").append(id);
        sb.append(", version=").append(version);
        sb.append(", content=").append(content);
        sb.append('}');
        return sb.toString();
    }
}
