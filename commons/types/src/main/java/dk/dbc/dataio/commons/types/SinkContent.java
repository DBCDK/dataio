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

import java.io.Serializable;

/**
 * SinkContent DTO class.
 */
public class SinkContent implements Serializable {
    private static final long serialVersionUID = -3413557101203220951L;

    private final String name;
    private final String resource;
    private final String description;

    /**
     * Class constructor
     *
     * @param name sink name
     * @param resource sink resource
     * @param description sink description
     *
     * @throws NullPointerException if given null-valued name or resource argument
     * @throws IllegalArgumentException if given empty-valued name or resource argument
     */
    @JsonCreator
    public SinkContent(@JsonProperty("name") String name,
                       @JsonProperty("resource") String resource,
                       @JsonProperty("description") String description) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.resource = InvariantUtil.checkNotNullNotEmptyOrThrow(resource, "resource");
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getResource() {
        return resource;
    }

    public String getDescription() {
        return description;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkContent)) return false;

        SinkContent that = (SinkContent) o;

        return name.equals(that.name)
                && resource.equals(that.resource)
                && !(description != null ? !description.equals(that.description) : that.description != null);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + resource.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
