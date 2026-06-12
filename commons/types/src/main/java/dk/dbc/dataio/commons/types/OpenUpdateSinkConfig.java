package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenUpdateSinkConfig implements SinkConfig, Serializable {

    private static final long serialVersionUID = 3950981967853410646L;

    private String userId;
    private String password;
    @JsonIgnore // This is set manually by the environment.
    private String groupId;
    @JsonIgnore // This is set manually by the environment.
    private Boolean validateOnly;
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

    public OpenUpdateSinkConfig withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public OpenUpdateSinkConfig withValidateOnly(Boolean validateOnly) {
        this.validateOnly = validateOnly;
        return this;
    }

    public Boolean isValidateOnly() {
        return validateOnly;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenUpdateSinkConfig that = (OpenUpdateSinkConfig) o;
        return Objects.equals(userId, that.userId)
                && Objects.equals(password, that.password)
                && Objects.equals(groupId, that.groupId)
                && Objects.equals(validateOnly, that.validateOnly)
                && Objects.equals(endpoint, that.endpoint)
                && Objects.equals(availableQueueProviders, that.availableQueueProviders)
                && Objects.equals(ignoredValidationErrors, that.ignoredValidationErrors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, password, groupId, validateOnly, endpoint, availableQueueProviders, ignoredValidationErrors);
    }

    @Override
    public String toString() {
        return "OpenUpdateSinkConfig{" +
                "userId='" + userId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", validateOnly=" + validateOnly +
                ", endpoint='" + endpoint + '\'' +
                ", availableQueueProviders=" + availableQueueProviders +
                ", ignoredValidationErrors=" + ignoredValidationErrors +
                '}';
    }
}
