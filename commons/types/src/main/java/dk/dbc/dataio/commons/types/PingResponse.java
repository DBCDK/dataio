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
import java.util.ArrayList;
import java.util.List;

/**
 * PingResponse DTO class.
 */
public class PingResponse implements Serializable {
    private static final long serialVersionUID = -1277715212626501064L;

    private final Status status;
    private final List<String> log;

    /**
     * Class constructor
     *
     * @param status ping result
     * @param log ping messages
     *
     * @throws NullPointerException if given null-valued status or log argument
     */
    @JsonCreator
    public PingResponse(@JsonProperty("status") PingResponse.Status status,
                        @JsonProperty("log") List<String> log) {

        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
        this.log = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(log, "log"));
    }

    public List<String> getLog() {
        return new ArrayList<>(log);
    }

    public Status getStatus() {
        return status;
    }

    public enum Status { OK, FAILED }
}
