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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Iso2709ReorderingDataPartitionerTest {
    private static final JobItemReorderer JOB_ITEM_REORDERER = mock(JobItemReorderer.class);

    private final Iso2709ReorderingDataPartitioner partitioner =
            Iso2709ReorderingDataPartitioner.newInstance(
                    StringUtil.asInputStream(""), "LATIN-1", JOB_ITEM_REORDERER);

    @Test
    public void hasNextDataPartitionerResult_jobItemReordererIsPartOfIteration() {
        when(JOB_ITEM_REORDERER.hasNext())
                .thenReturn(true)
                .thenReturn(false);
        assertThat(partitioner.hasNextDataPartitionerResult(), is(true));
        assertThat(partitioner.hasNextDataPartitionerResult(), is(false));
    }

    @Test
    public void nextDataPartitionerResult_reordererThrows_throws() {
        assertThat(partitioner::nextDataPartitionerResult, isThrowing(InvalidDataException.class));
    }
}