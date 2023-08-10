package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorldCatSinkConfig implements SinkConfig, Serializable {

    private static final long serialVersionUID = 1257505129736059983L;
    private String userId;
    private String password;
    private String endpoint;
    private String projectId;
    private List<String> retryDiagnostics;

    public String getUserId() {
        return userId;
    }

    public WorldCatSinkConfig withUserId(String userId) {
        this.userId = InvariantUtil.checkNotNullNotEmptyOrThrow(userId, "userId");
        return this;
    }

    public String getPassword() {
        return password;
    }

    public WorldCatSinkConfig withPassword(String password) {
        this.password = InvariantUtil.checkNotNullNotEmptyOrThrow(password, "password");
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public WorldCatSinkConfig withEndpoint(String endpoint) {
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public WorldCatSinkConfig withProjectId(String projectId) {
        this.projectId = InvariantUtil.checkNotNullNotEmptyOrThrow(projectId, "projectId");
        return this;
    }

    public List<String> getRetryDiagnostics() {
        return retryDiagnostics == null ? null : new ArrayList<>(retryDiagnostics);
    }

    public WorldCatSinkConfig withRetryDiagnostics(List<String> retryDiagnostics) {
        this.retryDiagnostics = InvariantUtil.checkNotNullOrThrow(retryDiagnostics, "retryDiagnostics");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldCatSinkConfig)) return false;

        WorldCatSinkConfig that = (WorldCatSinkConfig) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null) return false;
        if (projectId != null ? !projectId.equals(that.projectId) : that.projectId != null) return false;
        return retryDiagnostics != null ? retryDiagnostics.equals(that.retryDiagnostics) : that.retryDiagnostics == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        result = 31 * result + (projectId != null ? projectId.hashCode() : 0);
        result = 31 * result + (retryDiagnostics != null ? retryDiagnostics.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WorldCatSinkConfig{" +
                "userId='" + userId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", projectId='" + projectId + '\'' +
                ", retryDiagnostics=" + retryDiagnostics +
                '}';
    }
}
