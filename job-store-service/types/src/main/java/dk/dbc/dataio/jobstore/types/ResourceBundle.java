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
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class ResourceBundle {
    private final Flow flow;
    private final Sink sink;
    private final SupplementaryProcessData supplementaryProcessData;

    /**
     * Class constructor
     * @param flow cached within a job entity
     * @param sink cached within a job entity
     * @param supplementaryProcessData retrieved from job specification
     * @throws NullPointerException if given null-valued argument
     */
    @JsonCreator
    public ResourceBundle (@JsonProperty("flow") Flow flow,
                           @JsonProperty("sink") Sink sink,
                           @JsonProperty("supplementaryProcessData") SupplementaryProcessData supplementaryProcessData) throws NullPointerException {

        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.sink = InvariantUtil.checkNotNullOrThrow(sink, "sink");
        this.supplementaryProcessData = InvariantUtil.checkNotNullOrThrow(supplementaryProcessData, "supplementaryProcessData");
    }

    public Flow getFlow() {
        return flow;
    }

    public Sink getSink() {
        return sink;
    }

    public SupplementaryProcessData getSupplementaryProcessData() {
        return supplementaryProcessData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceBundle)) return false;

        ResourceBundle that = (ResourceBundle) o;

        if (!flow.equals(that.flow)) return false;
        if (!sink.equals(that.sink)) return false;
        if (!supplementaryProcessData.equals(that.supplementaryProcessData)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = flow.hashCode();
        result = 31 * result + sink.hashCode();
        result = 31 * result + supplementaryProcessData.hashCode();
        return result;
    }
}
