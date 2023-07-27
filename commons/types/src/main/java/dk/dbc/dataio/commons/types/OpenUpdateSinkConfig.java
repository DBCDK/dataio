package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenUpdateSinkConfig implements SinkConfig, Serializable {

    private static final long serialVersionUID = 3950981967853410646L;
    private String userId;
    private String password;
    private String endpoint;
    private List<String> availableQueueProviders;
    private Set<String> ignoredValidationErrors;

    public String getUserId() {
        return userId;
    }

    public OpenUpdateSinkConfig withUserId(String userId) {
        this.userId = InvariantUtil.checkNotNullNotEmptyOrThrow(userId, "userId");
        return this;
    }

    public String getPassword() {
        return password;
    }

    public OpenUpdateSinkConfig withPassword(String password) {
        this.password = InvariantUtil.checkNotNullNotEmptyOrThrow(password, "password");
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public OpenUpdateSinkConfig withEndpoint(String endpoint) {
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        return this;
    }

    public List<String> getAvailableQueueProviders() {
        return availableQueueProviders == null ? null : new ArrayList<>(availableQueueProviders);
    }

    public OpenUpdateSinkConfig withAvailableQueueProviders(List<String> availableQueueProviders) {
        this.availableQueueProviders = InvariantUtil.checkNotNullOrThrow(availableQueueProviders, "availableQueueProviders");
        return this;
    }

    public Set<String> getIgnoredValidationErrors() {
        return ignoredValidationErrors;
    }

    public OpenUpdateSinkConfig withIgnoredValidationErrors(Set<String> ignoredValidationErrors) {
        this.ignoredValidationErrors = ignoredValidationErrors;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OpenUpdateSinkConfig that = (OpenUpdateSinkConfig) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null) {
            return false;
        }
        if (availableQueueProviders != null ? !availableQueueProviders.equals(that.availableQueueProviders) : that.availableQueueProviders != null) {
            return false;
        }
        return ignoredValidationErrors != null ? ignoredValidationErrors.equals(that.ignoredValidationErrors) : that.ignoredValidationErrors == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        result = 31 * result + (availableQueueProviders != null ? availableQueueProviders.hashCode() : 0);
        result = 31 * result + (ignoredValidationErrors != null ? ignoredValidationErrors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OpenUpdateSinkConfig{" +
                "userId='" + userId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", availableQueueProviders=" + availableQueueProviders +
                ", ignoredValidationErrors=" + ignoredValidationErrors +
                '}';
    }
}
