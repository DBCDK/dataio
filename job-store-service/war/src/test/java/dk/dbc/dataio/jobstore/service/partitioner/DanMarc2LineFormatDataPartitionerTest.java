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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.marc.DanMarc2Charset;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DanMarc2LineFormatDataPartitionerTest {
    private static final String SPECIFIED_ENCODING = "latin1";
    private static final InputStream INPUT_STREAM = StringUtil.asInputStream("");

    @Test
    public void specifiedEncodingDiffersFromActualEncoding_throws() {
        try {
            DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, StandardCharsets.UTF_8.name());
            fail("No exception thrown");
        } catch (InvalidEncodingException ignored) { }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncoding_ok() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, SPECIFIED_ENCODING);
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingInLowerCase_ok() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, "LATIN1");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterTrim_ok() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, " latin1 ");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterDashReplace_ok() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, "latin-1");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void getEncoding_expectedEncodingReturned() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, SPECIFIED_ENCODING);
        assertThat("Encoding", dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void dm2LineFormatDataPartitioner_readValidRecord_returnsChunkItemWithMarcRecordAsMarcXchangeAndStatusSuccess() {
        final String simpleRecordInLineFormat = "245 00 *aA @*programmer is born*beveryday@@dbc\n";
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(
                StringUtil.asInputStream(simpleRecordInLineFormat, StandardCharsets.US_ASCII), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat("Valid input => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat(chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(chunkItem.getDiagnostics(), is(nullValue()));
        assertThat(dataPartitionerResult.getRecordInfo(), is(notNullValue()));
        assertThat("No more records => hasNext expected to be false", iterator.hasNext(), is(false));
    }

    @Test
    public void dm2LineFormatDataPartitioner_readInvalidRecord_returnsChunkItemWithFaultyRecordsAsDataAndStatusFailure() {
        final String faultyRecordInLineFormat = "245 00 *aA @*programmer is *\n";
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(
                StringUtil.asInputStream(faultyRecordInLineFormat, StandardCharsets.US_ASCII), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat(chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(new String(chunkItem.getData(), StandardCharsets.UTF_8), is(faultyRecordInLineFormat));
        assertThat(chunkItem.getDiagnostics().size(), is(1));
        assertThat(dataPartitionerResult.getRecordInfo(), is(nullValue()));
        assertThat("No more records => hasNext expected to be false", iterator.hasNext(), is(false));
    }

    @Test
    public void dm2LineFormatDataPartitioner_readInvalidRecordNotRecognisedAsLineFormat_throwsInvalidDataException() {
        final String faultyRecord = "*aA @*programmer is born";
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(
                StringUtil.asInputStream(faultyRecord, new DanMarc2Charset()), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (PrematureEndOfDataException ignored) { }
    }

    @Test
    public void dm2LineFormatDataPartitioner_multipleIterations() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(
                getClass().getResourceAsStream("/test-records-74-danmarc2.lin"), SPECIFIED_ENCODING);
        int chunkItemNo = 0;
        for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
            assertThat("result" + chunkItemNo + " position in datafile",
                    dataPartitionerResult.getPositionInDatafile(), is(chunkItemNo));
            assertThat("Chunk item " + chunkItemNo++, dataPartitionerResult.getChunkItem(), is(notNullValue()));
        }
        assertThat("Number of chunk item created", chunkItemNo, is(74));
        assertThat("Number of bytes read", dataPartitioner.getBytesRead(), is(71516L));
    }

    @Test
    public void dm2LineFormatDataPartitioner_drain40() {
        final DataPartitioner dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(
                getClass().getResourceAsStream("/test-records-74-danmarc2.lin"), SPECIFIED_ENCODING);
        int chunkItemNo = 40;
        dataPartitioner.drainItems(chunkItemNo);
        for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
            assertThat("result" + chunkItemNo + " position in datafile",
                    dataPartitionerResult.getPositionInDatafile(), is(chunkItemNo));
            assertThat("Chunk item " + chunkItemNo++, dataPartitionerResult.getChunkItem(), is(notNullValue()));
        }
        assertThat("Number of chunk item created", chunkItemNo - 40, is(34));
        assertThat("Number of bytes read", dataPartitioner.getBytesRead(), is(71516L));
    }


    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            DanMarc2LineFormatDataPartitioner.newInstance(null, SPECIFIED_ENCODING);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        try {
            DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        try {
            DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void newInstance_allArgsAreValid_returnsNewDataPartitioner() {
        assertThat(DanMarc2LineFormatDataPartitioner.newInstance(INPUT_STREAM, SPECIFIED_ENCODING), is(notNullValue()));
    }
}
