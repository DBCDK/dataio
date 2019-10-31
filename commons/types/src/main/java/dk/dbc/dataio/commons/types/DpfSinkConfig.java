/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */
package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DpfSinkConfig implements SinkConfig, Serializable {
    private String userId;
    private String password;
    private List<String> availableQueueProviders;

    public String getUserId() {
        return userId;
    }

    public DpfSinkConfig withUserId(String userId) {
        this.userId = InvariantUtil.checkNotNullNotEmptyOrThrow(userId, "userId");
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DpfSinkConfig withPassword(String password) {
        this.password = InvariantUtil.checkNotNullNotEmptyOrThrow(password, "password");
        return this;
    }

    public List<String> getAvailableQueueProviders() {
        return availableQueueProviders == null ? null : new ArrayList<>(availableQueueProviders);
    }

    public DpfSinkConfig withAvailableQueueProviders(List<String> availableQueueProviders) {
        this.availableQueueProviders = InvariantUtil.checkNotNullOrThrow(availableQueueProviders, "availableQueueProviders");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DpfSinkConfig)) return false;

        DpfSinkConfig that = (DpfSinkConfig) o;
        if (!Objects.equals(userId, that.userId)) return false;
        if (!Objects.equals(password, that.password)) return false;
        return Objects.equals(availableQueueProviders, that.availableQueueProviders);
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (availableQueueProviders != null ? availableQueueProviders.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DpfSinkConfig{" +
                "userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", availableQueueProviders=" + availableQueueProviders +
                '}';
    }
}
