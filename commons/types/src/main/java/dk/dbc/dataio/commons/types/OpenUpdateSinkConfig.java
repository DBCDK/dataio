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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenUpdateSinkConfig implements SinkConfig {

    private final String userId;
    private final String password;
    private final String endpoint;
    private final List<String> availableQueueProviders;  // Optional

    @JsonCreator
    public OpenUpdateSinkConfig(@JsonProperty("userId") String userId,
                                @JsonProperty("password") String password,
                                @JsonProperty("endpoint") String endpoint,
                                @JsonProperty("queueProviders") List<String> availableQueueProviders) {
        this.userId = InvariantUtil.checkNotNullNotEmptyOrThrow(userId, "userId");
        this.password = InvariantUtil.checkNotNullNotEmptyOrThrow(password, "password");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
        this.availableQueueProviders = availableQueueProviders == null ? null : new ArrayList<>(availableQueueProviders);
    }

    public OpenUpdateSinkConfig(String userId, String password, String endpoint) {
        this(userId, password, endpoint, null);
    }

    public String getPassword() {
        return password;
    }

    public String getUserId() {
        return userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public List<String> getAvailableQueueProviders() {
        return availableQueueProviders == null ? null : Collections.unmodifiableList(availableQueueProviders);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpenUpdateSinkConfig)) return false;

        OpenUpdateSinkConfig that = (OpenUpdateSinkConfig) o;

        if (!userId.equals(that.userId)) return false;
        if (!password.equals(that.password)) return false;
        if (!endpoint.equals(that.endpoint)) return false;
        return availableQueueProviders != null ? availableQueueProviders.equals(that.availableQueueProviders) : that.availableQueueProviders == null;
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + endpoint.hashCode();
        result = 31 * result + (availableQueueProviders != null ? availableQueueProviders.hashCode() : 0);
        return result;
    }
}
