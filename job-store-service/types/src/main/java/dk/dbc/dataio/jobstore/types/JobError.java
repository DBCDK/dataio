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

package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

/**
 * Class representing a job error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobError {
    public static final String NO_STACKTRACE = null;
    public enum Code {
        INVALID_DATA,
        INVALID_ITEM_IDENTIFIER,
        INVALID_JOB_IDENTIFIER,
        INVALID_JOB_SPECIFICATION,
        ILLEGAL_CHUNK,
        INVALID_INPUT,
        INVALID_JSON,
        INVALID_CHUNK_IDENTIFIER,
        INVALID_CHUNK_TYPE,
        FORBIDDEN_SINK_TYPE_TICKLE
    }

    private final @JsonProperty Code code;
    private final @JsonProperty String description;
    private final @JsonProperty String stacktrace;

    /**
     * Class constructor
     * @param code error code
     * @param description error description
     * @param stacktrace error stacktrace
     * @throws NullPointerException if given null-valued code
     */
    @JsonCreator
    public JobError(
            @JsonProperty("code") Code code,
            @JsonProperty("description") String description,
            @JsonProperty("stacktrace") String stacktrace) throws NullPointerException, IllegalArgumentException {
        this.code = InvariantUtil.checkNotNullOrThrow(code, "code");
        this.description = description;
        this.stacktrace = stacktrace;
    }

    public JobError(Code code, String description) {
        this(code, description, null);
    }

    public JobError(Code code) {
        this(code, null, null);
    }

    public Code getCode() {
        return code;
    }

    @JsonIgnore
    public String getDescription() {
        return description == null ? "" : description;
    }

    @JsonIgnore
    public String getStacktrace() {
        return stacktrace == null ? "" : stacktrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobError jobError = (JobError) o;

        if (code != jobError.code) {
            return false;
        }
        if (description != null ? !description.equals(jobError.description) : jobError.description != null) {
            return false;
        }
        return stacktrace != null ? stacktrace.equals(jobError.stacktrace) : jobError.stacktrace == null;
    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        return result;
    }
}
