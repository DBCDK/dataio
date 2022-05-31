package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImsSinkConfig implements SinkConfig, Serializable {
    private static final long serialVersionUID = 1257505129736059671L;

    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }

    public ImsSinkConfig withEndpoint(String endpoint) {
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImsSinkConfig)) return false;

        ImsSinkConfig that = (ImsSinkConfig) o;

        return endpoint != null ? endpoint.equals(that.endpoint) : that.endpoint == null;

    }

    @Override
    public int hashCode() {
        return endpoint != null ? endpoint.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ImsSinkConfig{" +
                "endpoint='" + endpoint + '\'' +
                '}';
    }
}
