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

public class OpenUpdateSinkConfig implements SinkConfig {

    private final String userId;
    private final String password;
    private final String endpoint;

    @JsonCreator
    public OpenUpdateSinkConfig(@JsonProperty("userId") String userId,
                                @JsonProperty("password") String password,
                                @JsonProperty("endpoint") String endpoint) {

        this.userId = InvariantUtil.checkNotNullNotEmptyOrThrow(userId, "userId");
        this.password = InvariantUtil.checkNotNullNotEmptyOrThrow(password, "password");
        this.endpoint = InvariantUtil.checkNotNullNotEmptyOrThrow(endpoint, "endpoint");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpenUpdateSinkConfig)) return false;

        OpenUpdateSinkConfig that = (OpenUpdateSinkConfig) o;

        return userId.equals(that.userId)
                && password.equals(that.password)
                && endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + endpoint.hashCode();
        return result;
    }
}
