/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class OpenUpdateSinkConfig implements SinkConfig {

    private String userId;
    private String password;
    private String endpoint;
    private List<String> availableQueueProviders;

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
        this.availableQueueProviders = availableQueueProviders;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpenUpdateSinkConfig)) return false;

        OpenUpdateSinkConfig that = (OpenUpdateSinkConfig) o;

        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null) return false;
        return availableQueueProviders != null ? availableQueueProviders.equals(that.availableQueueProviders) : that.availableQueueProviders == null;

    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        result = 31 * result + (availableQueueProviders != null ? availableQueueProviders.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OpenUpdateSinkConfig{" +
                "userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", availableQueueProviders=" + availableQueueProviders +
                '}';
    }
}
