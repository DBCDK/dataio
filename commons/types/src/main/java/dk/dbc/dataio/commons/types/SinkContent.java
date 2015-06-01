package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * SinkContent DTO class.
 */
public class SinkContent implements Serializable {
    private static final long serialVersionUID = -3413557101203220951L;

    private final String name;
    private final String resource;

    /**
     * Class constructor
     *
     * @param name sink name
     * @param resource sink resource
     *
     * @throws NullPointerException if given null-valued name or resource argument
     * @throws IllegalArgumentException if given empty-valued name or resource argument
     */
    @JsonCreator
    public SinkContent(@JsonProperty("name") String name,
                       @JsonProperty("resource") String resource) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.resource = InvariantUtil.checkNotNullNotEmptyOrThrow(resource, "resource");
    }

    public String getName() {
        return name;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkContent)) return false;

        SinkContent that = (SinkContent) o;

        if (!name.equals(that.name)) return false;
        if (!resource.equals(that.resource)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + resource.hashCode();
        return result;
    }
}
