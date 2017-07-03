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
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DsdCsvDataPartitionerTest {
    @Test
    public void readingNextRecordFromEmptyStream() throws IOException {
        final DsdCsvDataPartitioner partitioner =
                DsdCsvDataPartitioner.newInstance(StringUtil.asInputStream(""), StandardCharsets.UTF_8.name());
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat(dataPartitionerResult, is(DataPartitionerResult.EMPTY));
    }

    @Test
    public void partitioningCSV() throws IOException {
        final String csvRecords =
                "a,\"b, with whitespace and comma\",\n" +
                "\"d has unbalanced\"\",and,fails\n" +
                "\"\"\"g\"\"\",\"h contains<p><a href=\"\"url\"\">html</a>\"";

        final DsdCsvDataPartitioner partitioner =
                DsdCsvDataPartitioner.newInstance(StringUtil.asInputStream(csvRecords), StandardCharsets.UTF_8.name());

        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();

        assertThat("has 1st result", iterator.hasNext(), is(true));
        DataPartitionerResult result = iterator.next();
        assertThat("1st result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("1st result chunk item data", StringUtil.asString(result.getChunkItem().getData()),
                is("<csv><line><C0>a</C0><C1>b, with whitespace and comma</C1><C2></C2></line></csv>"));
        assertThat("1st result record info", result.getRecordInfo(), is(nullValue()));
        assertThat("1st result position in datafile", result.getPositionInDatafile(), is(0));

        assertThat("has 2nd result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("2nd result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd result chunk item data", StringUtil.asString(result.getChunkItem().getData()),
                is("\"d has unbalanced\"\",and,fails"));
        assertThat("2nd result chunk item has diagnostic",
                !result.getChunkItem().getDiagnostics().isEmpty(), is(true));
        assertThat("2nd result record info", result.getRecordInfo(), is(nullValue()));
        assertThat("2nd result position in datafile", result.getPositionInDatafile(), is(1));

        assertThat("has 3rd result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("3rd result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("3rd result chunk item data", StringUtil.asString(result.getChunkItem().getData()),
                is("<csv><line><C0>\"g\"</C0><C1>h contains html</C1></line></csv>"));
        assertThat("3rd result record info", result.getRecordInfo(), is(nullValue()));
        assertThat("3rd result position in datafile", result.getPositionInDatafile(), is(2));

        assertThat("no more records", iterator.hasNext(), is(false));

        assertThat("calling next() after hasNext() returns false",
                iterator.next(), is(DataPartitionerResult.EMPTY));

        assertThat("bytes counted", partitioner.getBytesRead() > 0, is(true));
    }

    @Test
    public void drainItems() {
        final String csvRecords =
                "a,b\n" +
                "c,d\n" +
                "e,f\n" +
                "g,h\n";

        final DsdCsvDataPartitioner partitioner =
                DsdCsvDataPartitioner.newInstance(StringUtil.asInputStream(csvRecords), StandardCharsets.UTF_8.name());
        partitioner.drainItems(3);

        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();

        assertThat("has 4th result", iterator.hasNext(), is(true));
        DataPartitionerResult result = iterator.next();
        assertThat("4th result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("4th result chunk item data", StringUtil.asString(result.getChunkItem().getData()),
                is("<csv><line><C0>g</C0><C1>h</C1></line></csv>"));
        assertThat("4th result record info", result.getRecordInfo(), is(nullValue()));
        assertThat("4th result position in datafile", result.getPositionInDatafile(), is(3));
    }

    @Test
    public void prematureEndOfData() throws IOException {
        final InputStream is = mock(InputStream.class);
        when(is.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException());

        final DsdCsvDataPartitioner partitioner = DsdCsvDataPartitioner.newInstance(is, StandardCharsets.UTF_8.name());
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("hasNext() defers handling of IOException", iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(PrematureEndOfDataException.class));
    }
}