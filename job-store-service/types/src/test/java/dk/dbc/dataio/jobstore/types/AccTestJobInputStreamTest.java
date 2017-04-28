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

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AccTestJobInputStreamTest {

    private final JobSpecification jobSpecification = new JobSpecification().withFormat("format");
    private final Flow flow = new FlowBuilder().build();
    private final RecordSplitterConstants.RecordSplitter typeOfDataPartitioner = RecordSplitterConstants.RecordSplitter.XML;

    @Test
    public void constructor_flowArgIsNull_throws() {
        assertThat(() -> new AccTestJobInputStream(jobSpecification, null, typeOfDataPartitioner),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_typeOfDataPartitionerArgIsNull_throws() {
        assertThat(() -> new AccTestJobInputStream(jobSpecification, flow, null),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returns() {
        AccTestJobInputStream jobInputStream = new AccTestJobInputStream(jobSpecification, flow, typeOfDataPartitioner);
        assertThat(jobInputStream.getFlow(), is(flow));
        assertThat(jobInputStream.getTypeOfDataPartitioner(), is(typeOfDataPartitioner));
        assertThat(jobInputStream.getJobSpecification(), is(jobSpecification));
        assertThat(jobInputStream.getIsEndOfJob(), is(false)); // Default value
        assertThat(jobInputStream.getPartNumber(), is(0));     // Default value
    }
}
