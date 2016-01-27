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

import dk.dbc.dataio.commons.types.ChunkItem;
import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RawRepoMarcXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(null, getUft8Encoding());
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void newInstance_allArgsAreValid_returnsNewDataPartitioner() {
        assertThat(RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), getUft8Encoding()), is(notNullValue()));
    }

    @Test
    public void iterator_next_returnsThreeChunkItemsWithTrackingIdSet() {
        final DataPartitioner dataPartitioner = newPartitionerInstance(getDataContainerXmlWithMarcExchangeAndTrackingIds());
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem0 = iterator.next();
        assertThat("chunkItem0.trackingId", chunkItem0.getTrackingId(), is("trackingid-dataio-1"));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem1 = iterator.next();
        assertThat("chunkItem1.trackingId", chunkItem1.getTrackingId(), is("trackingid-dataio-2"));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem2 = iterator.next();
        assertThat("chunkItem1.trackingId", chunkItem2.getTrackingId(), is("trackingid-dataio-3"));
        assertThat(iterator.hasNext(), is(false));
    }

    private DataPartitioner newPartitionerInstance(String xml) {
        return RawRepoMarcXmlDataPartitioner.newInstance(asInputStream(xml), getUft8Encoding());
    }

}
