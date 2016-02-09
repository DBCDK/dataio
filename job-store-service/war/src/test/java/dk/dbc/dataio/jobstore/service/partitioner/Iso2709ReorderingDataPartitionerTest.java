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

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Iso2709ReorderingDataPartitionerTest {
    private static final InputStream INPUT_STREAM = StringUtil.asInputStream("");
    private static final String ENCODING = "latin1";
    private static final JobItemReorderer JOB_ITEM_REORDERER = mock(JobItemReorderer.class);

    private final Iso2709ReorderingDataPartitioner partitioner =
            Iso2709ReorderingDataPartitioner.newInstance(INPUT_STREAM, ENCODING, JOB_ITEM_REORDERER);

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            Iso2709ReorderingDataPartitioner.newInstance(null, ENCODING, JOB_ITEM_REORDERER);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        try {
            Iso2709ReorderingDataPartitioner.newInstance(INPUT_STREAM, null, JOB_ITEM_REORDERER);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        try {
            Iso2709ReorderingDataPartitioner.newInstance(INPUT_STREAM, "", JOB_ITEM_REORDERER);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void newInstance_jobItemReordererArgIsNull_throws() {
        try {
            Iso2709ReorderingDataPartitioner.newInstance(INPUT_STREAM, ENCODING, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_allArgsAreValid_returnsNewDataPartitioner() {
        assertThat(Iso2709ReorderingDataPartitioner.newInstance(INPUT_STREAM, ENCODING, JOB_ITEM_REORDERER), is(notNullValue()));
    }

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
        try {
            partitioner.nextDataPartitionerResult();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }
}