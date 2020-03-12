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
import dk.dbc.invariant.InvariantUtil;

/**
 * Class representing a flow store error
 */
public class FlowStoreError {
    public enum Code {
        NONEXISTING_SUBMITTER,
        EXISTING_SUBMITTER_NONEXISTING_DESTINATION,
        EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
        INVALID_QUERY
    }

    private final Code code;
    private final String description;
    private final String stacktrace;

    /**
     * Class constructor
     * @param code error code
     * @param description error description
     * @param stacktrace error stacktrace or empty string if given as null or empty string
     * @throws NullPointerException if given null-valued code or description argument
     * @throws IllegalArgumentException if given empty valued description argument
     */
    @JsonCreator
    public FlowStoreError(
            @JsonProperty("code") Code code,
            @JsonProperty("description") String description,
            @JsonProperty("stacktrace") String stacktrace) throws NullPointerException, IllegalArgumentException {
        this.code = InvariantUtil.checkNotNullOrThrow(code, "code");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.stacktrace = stacktrace == null ? "" : stacktrace;
    }

    /**
     * Gets the error code
     * @return error code
     */
    public Code getCode() {
        return code;
    }

    /**
     * Gets the description
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the stacktrace
     * @return stacktrace
     */
    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowStoreError)) return false;

        FlowStoreError that = (FlowStoreError) o;

        if (code != that.code) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return !(stacktrace != null ? !stacktrace.equals(that.stacktrace) : that.stacktrace != null);

    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FlowStoreError{" +
                "code=" + code +
                ", description='" + description + '\'' +
                ", stacktrace='" + stacktrace + '\'' +
                '}';
    }

}
