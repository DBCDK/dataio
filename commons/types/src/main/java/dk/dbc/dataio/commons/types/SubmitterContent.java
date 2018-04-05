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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * SubmitterContent DTO class.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class SubmitterContent implements Serializable {
    private static final long serialVersionUID = -2754982619041504537L;

    private final long number;
    private final String name;
    private final String description;
    private final Priority priority;
    private final boolean enabled;

    /**
     * Class constructor
     *
     * @param number submitter number (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param name submitter name
     * @param description submitter description
     * @param priority priority of this submitter
     * @param enabled flag setting the status of the submitter (enabled or disabled for job creation)
     *
     * @throws NullPointerException if given null-valued name or description argument
     * @throws IllegalArgumentException if given empty-valued name or description argument, or if
     * value of number is not larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND}
     */
    @JsonCreator
    public SubmitterContent(@JsonProperty("number") long number,
                            @JsonProperty("name") String name,
                            @JsonProperty("description") String description,
                            @JsonProperty("priority") Priority priority,
                            @JsonProperty("enabled") boolean enabled) {

        this.number = InvariantUtil.checkLowerBoundOrThrow(number, "number", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.priority = priority;
        this.enabled = enabled;
    }

    public long getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmitterContent)) return false;

        SubmitterContent that = (SubmitterContent) o;

        if (number != that.number) return false;
        if (enabled != that.enabled) return false;
        if (!name.equals(that.name)) return false;
        if (!description.equals(that.description)) return false;
        return priority == that.priority;
    }

    @Override
    public int hashCode() {
        int result = (int) (number ^ (number >>> 32));
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + (priority != null ? priority.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }
}