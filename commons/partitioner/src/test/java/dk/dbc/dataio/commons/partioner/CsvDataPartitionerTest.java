package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvDataPartitionerTest {
    @Test
    public void readingNextRecordFromEmptyStream() {
        CsvDataPartitioner partitioner = newCsvDataPartitionerFor("");
        Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat(dataPartitionerResult, is(DataPartitionerResult.EMPTY));
    }

    @Test
    public void partitioningCSV() {
        final String csvRecords =
                "a,\"b, with whitespace and comma\",\n" +
                        "\"d has unbalanced\"\",and,fails\n" +
                        "\"\"\"g\"\"\",\"h contains<p><a href=\"\"url\"\">html</a>\"";

        CsvDataPartitioner partitioner = newCsvDataPartitionerFor(csvRecords);

        Iterator<DataPartitionerResult> iterator = partitioner.iterator();

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
        assertThat("2nd result has ERROR level diagnostic",
                result.getChunkItem().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.ERROR));
        assertThat("2nd result record info", result.getRecordInfo(), is(nullValue()));
        assertThat("2nd result position in datafile", result.getPositionInDatafile(), is(1));

        assertThat("has 3rd result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("3rd result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("3rd result chunk item data", StringUtil.asString(result.getChunkItem().getData()),
                is("<csv><line><C0>\"g\"</C0><C1>h contains&lt;p&gt;&lt;a href=\"url\"&gt;html&lt;/a&gt;</C1></line></csv>"));
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

        CsvDataPartitioner partitioner = newCsvDataPartitionerFor(csvRecords);
        partitioner.drainItems(3);

        Iterator<DataPartitionerResult> iterator = partitioner.iterator();

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
        InputStream is = mock(InputStream.class);
        when(is.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException());

        CsvDataPartitioner partitioner = CsvDataPartitioner.newInstance(is, StandardCharsets.UTF_8.name());
        Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("hasNext() defers handling of IOException", iterator.hasNext(), is(true));
        Assertions.assertThrows(PrematureEndOfDataException.class, iterator::next);
    }

    private CsvDataPartitioner newCsvDataPartitionerFor(String csv) {
        return CsvDataPartitioner.newInstance(
                StringUtil.asInputStream(csv), StandardCharsets.UTF_8.name());
    }
}
