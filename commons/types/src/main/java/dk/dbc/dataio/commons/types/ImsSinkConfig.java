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
