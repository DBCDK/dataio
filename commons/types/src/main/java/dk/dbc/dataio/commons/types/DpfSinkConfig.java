package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DpfSinkConfig implements SinkConfig, Serializable {
    private String updateServiceUserId;
    private String updateServicePassword;
    private List<String> updateServiceAvailableQueueProviders;

    public String getUpdateServiceUserId() {
        return updateServiceUserId;
    }

    public DpfSinkConfig withUpdateServiceUserId(String updateServiceUserId) {
        this.updateServiceUserId = InvariantUtil.checkNotNullNotEmptyOrThrow(updateServiceUserId, "updateServiceUserId");
        return this;
    }

    public String getUpdateServicePassword() {
        return updateServicePassword;
    }

    public DpfSinkConfig withUpdateServicePassword(String updateServicePassword) {
        this.updateServicePassword = InvariantUtil.checkNotNullNotEmptyOrThrow(updateServicePassword, "updateServicePassword");
        return this;
    }

    public List<String> getUpdateServiceAvailableQueueProviders() {
        return updateServiceAvailableQueueProviders == null ?
                null : new ArrayList<>(updateServiceAvailableQueueProviders);
    }

    public DpfSinkConfig withUpdateServiceAvailableQueueProviders(List<String> updateServiceAvailableQueueProviders) {
        this.updateServiceAvailableQueueProviders = InvariantUtil.checkNotNullOrThrow(
                updateServiceAvailableQueueProviders, "updateServiceAvailableQueueProviders");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DpfSinkConfig)) return false;

        DpfSinkConfig that = (DpfSinkConfig) o;
        if (!Objects.equals(updateServiceUserId, that.updateServiceUserId)) return false;
        if (!Objects.equals(updateServicePassword, that.updateServicePassword)) return false;
        return Objects.equals(updateServiceAvailableQueueProviders, that.updateServiceAvailableQueueProviders);
    }

    @Override
    public int hashCode() {
        int result = updateServiceUserId != null ? updateServiceUserId.hashCode() : 0;
        result = 31 * result + (updateServicePassword != null ? updateServicePassword.hashCode() : 0);
        result = 31 * result + (updateServiceAvailableQueueProviders != null ?
                updateServiceAvailableQueueProviders.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DpfSinkConfig{" +
                "userId='" + updateServiceUserId + '\'' +
                ", password='" + updateServicePassword + '\'' +
                ", availableQueueProviders=" + updateServiceAvailableQueueProviders +
                '}';
    }
}
