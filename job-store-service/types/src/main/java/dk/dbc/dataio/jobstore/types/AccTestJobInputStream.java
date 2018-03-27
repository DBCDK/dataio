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

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.invariant.InvariantUtil;

public class AccTestJobInputStream extends JobInputStream {

    private final Flow flow;
    private final RecordSplitterConstants.RecordSplitter typeOfDataPartitioner;

    /**
     * @param jobSpecification the jobSpecification
     * @param flow             the flow to use
     * @param typeOfDataPartitioner the type of data partitioner to use
     * @throws NullPointerException if given null-valued argument
     */
    public AccTestJobInputStream(
            @JsonProperty("jobSpecification") JobSpecification jobSpecification,
            @JsonProperty("flow")Flow flow,
            @JsonProperty("typeOfDataPartitioner") RecordSplitterConstants.RecordSplitter typeOfDataPartitioner) throws NullPointerException {

        super(jobSpecification);
        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.typeOfDataPartitioner = InvariantUtil.checkNotNullOrThrow(typeOfDataPartitioner, "typeOfDataPartitioner");
    }

    public Flow getFlow() {
        return flow;
    }

    public RecordSplitterConstants.RecordSplitter getTypeOfDataPartitioner() {
        return typeOfDataPartitioner;
    }
}
