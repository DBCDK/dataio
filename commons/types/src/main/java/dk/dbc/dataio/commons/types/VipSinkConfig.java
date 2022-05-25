package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VipSinkConfig implements SinkConfig, Serializable {
    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }

    public VipSinkConfig withEndpoint(String endpoint) {
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(
                endpoint, "endpoint");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VipSinkConfig)) return false;
        VipSinkConfig that = (VipSinkConfig) o;
        return Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return endpoint != null ? endpoint.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "VipSinkConfig{" +
                "endpoint='" + endpoint + '\'' +
                '}';
    }
}
