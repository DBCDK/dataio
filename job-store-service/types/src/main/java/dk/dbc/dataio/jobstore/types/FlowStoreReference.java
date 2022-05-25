package dk.dbc.dataio.jobstore.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

public class FlowStoreReference {

    private final long id;
    private final long version;
    private final String name;

    @JsonCreator
    public FlowStoreReference(@JsonProperty("id") long id,
                              @JsonProperty("version") long version,
                              @JsonProperty("name") String name) throws NullPointerException {
        this.id = id;
        this.version = version;
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
    }

    /*
     * Getters
     */
    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowStoreReference)) return false;

        FlowStoreReference that = (FlowStoreReference) o;

        if (id != that.id) return false;
        if (version != that.version) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + name.hashCode();
        return result;
    }
}
