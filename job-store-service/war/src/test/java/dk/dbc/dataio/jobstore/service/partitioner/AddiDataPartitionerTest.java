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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class AddiDataPartitionerTest {
    private final static InputStream EMPTY_STREAM = StringUtil.asInputStream("");
    private final static String UTF_8_ENCODING = "UTF-8";

    @Test(expected = NullPointerException.class)
    public void constructor_inputStreamArgIsNull_throws() {
        new AddiDataPartitionerImpl(null, UTF_8_ENCODING);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_encodingNameArgIsNull_throws() {
        new AddiDataPartitionerImpl(EMPTY_STREAM, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_encodingNameArgIsEmpty_throws() {
        new AddiDataPartitionerImpl(EMPTY_STREAM, " ");
    }

    @Test(expected = InvalidEncodingException.class)
    public void constructor_encodingNameArgIsInvalid_throws() {
        new AddiDataPartitionerImpl(EMPTY_STREAM, "no-such-encoding");
    }

    @Test
    public void partitioner_readingNextRecordFromEmptyStream_returnsEmptyResult() {
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(EMPTY_STREAM, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat(dataPartitionerResult, is(DataPartitionerResult.EMPTY));
    }

    @Test
    public void partitioner_readingInvalidAddiFormat_throws() {
        final InputStream addiStream = StringUtil.asInputStream("2\n{}\n");
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void partitioner_readingValidRecord_returnsResultWithChunkItemWithStatusSuccess() {
        final InputStream addiStream = StringUtil.asInputStream("2\n{}\n7\ncontent\n");
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem.getDiagnostics()", chunkItem.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem.getData", StringUtil.asString(chunkItem.getData()), is("content"));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.UNKNOWN)));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(notNullValue()));
    }

    @Test
    public void partitioner_readingEmptyRecord_returnsResultWithChunkItemWithStatusIgnore() {
        final InputStream addiStream = StringUtil.asInputStream("0\n\n0\n\n");
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.STRING)));
    }

    @Test
    public void partitioner_readingRecordWithInvalidMetaData_returnsResultWithChunkItemWithStatusFailure() {
        final InputStream addiStream = StringUtil.asInputStream("8\nnot json\n7\ncontent\n");
        final AddiRecord expectedContent = new AddiRecord(StringUtil.asBytes("not json"), StringUtil.asBytes("content"));
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem.getDiagnostics()", chunkItem.getDiagnostics(), is(notNullValue()));
        assertThat("chunkItem.getData", StringUtil.asString(chunkItem.getData()), is(StringUtil.asString(expectedContent.getBytes())));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.BYTES)));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(nullValue()));
    }

    @Test
    public void partitioner_MetaDataContainsTrackingId_returnsResultWithChunkItemWithTrackingId() {
        final InputStream addiStream = StringUtil.asInputStream("27\n{\"trackingId\": \"trackedAs\"}\n7\ncontent\n");
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getTrackingId()", chunkItem.getTrackingId(), is("trackedAs"));
    }

    @Test
    public void partitioner_multipleIterations() {
        final InputStream addiStream = StringUtil.asInputStream("2\n{}\n8\ncontent1\n2\n{}\n8\ncontent2\n");
        final AddiDataPartitionerImpl partitioner = new AddiDataPartitionerImpl(addiStream, UTF_8_ENCODING);
        int chunkItemNo = 0;
        for (DataPartitionerResult dataPartitionerResult : partitioner) {
            final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
            chunkItemNo++;
            assertThat("Chunk item " + chunkItemNo, chunkItem, is(notNullValue()));
        }
        assertThat("Number of chunk item created", chunkItemNo, is(2));
        assertThat("Number of bytes read", partitioner.getBytesRead(), is(32L));
    }

    private static class AddiDataPartitionerImpl extends AddiDataPartitioner {
        public AddiDataPartitionerImpl(InputStream inputStream, String encodingName)
                throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
            super(inputStream, encodingName);
        }

        @Override
        protected ChunkItem.Type getChunkItemType() {
            return ChunkItem.Type.UNKNOWN;
        }
    }
}