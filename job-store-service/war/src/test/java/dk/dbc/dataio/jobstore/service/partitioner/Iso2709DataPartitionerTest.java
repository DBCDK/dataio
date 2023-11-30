package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.parsers.ParserConfigurationException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.xmlunit.builder.Input.fromByteArray;
import static org.xmlunit.builder.Input.fromStream;

public class Iso2709DataPartitionerTest extends AbstractPartitionerTestBase {
    private final static String INPUT_RECORD_1_ISO = "test-record-1-danmarc2.iso";
    private final static String INPUT_BROKEN_ISO = "broken-iso2709-2.iso";
    private final static String INPUT_RECORDS_3_ISO = "test-records-3-danmarc2.iso";
    private final static String INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO = "test-records-4-danmarc2-guard-against-infinite-iteration.iso";
    private final static String INPUT_RECORDS_4_ERROR_IN_RECORD2 = "test-records-4-error-in-record2.iso";
    private final static String INPUT_RECORDS_323_MARC21_UTF8_ISO = "test-records-323-marc21-utf8.iso";
    private final static String OUTPUT_RECORD_1_MARCXCHANGE = "test-record-1-danmarc2.marcXChange";
    private final static String DEFAULT_RECORD_ID = "30769430";

    @Test
    public void getEncoding_expectedEncodingReturned() {
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getEmptyInputStream(), "LATIN-1");
        assertThat("Encoding", dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void iso2709DataPartitioner_oneValidRecord_accepted() {
        byte[] isoRecord = getResourceAsByteArray(INPUT_RECORD_1_ISO);
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORD_1_ISO), "LATIN-1");
        Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat("First dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat("chunkItem0.data matches expected output String", fromByteArray(dataPartitionerResult.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo1.id", dataPartitionerResult.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));
        assertThat("No more dataPartitionerResults => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecord.length, dataPartitioner.getBytesRead(), is((long) isoRecord.length));
    }

    @Test
    public void iso2709DataPartitioner_multipleRecords_accepted() {
        byte[] isoRecords = getResourceAsByteArray(INPUT_RECORDS_3_ISO);
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_3_ISO), "LATIN-1");
        Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat("First dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        DataPartitionerResult dataPartitionerResult0 = iterator.next();
        assertThat("chunkItem0.data matches expected output String", fromByteArray(dataPartitionerResult0.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo0.id", dataPartitionerResult0.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("Second dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        DataPartitionerResult dataPartitionerResult1 = iterator.next();
        assertThat("chunkItem1 matches expected output String", fromByteArray(dataPartitionerResult1.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo1.id", dataPartitionerResult1.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("Third record => hasNext() expected to be true", iterator.hasNext(), is(true));
        DataPartitionerResult dataPartitionerResult2 = iterator.next();
        assertThat("chunkItem2.data matches expected output String", fromByteArray(dataPartitionerResult2.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo2.id", dataPartitionerResult2.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("No more dataPartitionerResults => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecords.length, dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test
    public void iso2709DataPartitioner_invalidIso2709_throws() throws ParserConfigurationException {
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_BROKEN_ISO), "LATIN-1");
        Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        // 2 good records
        assertThat("First dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        DataPartitionerResult dataPartitionerResult0 = iterator.next();
        assertThat("chunkItem0.data matches expected output String", fromByteArray(dataPartitionerResult0.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo0.id", dataPartitionerResult0.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("Second dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        DataPartitionerResult dataPartitionerResult1 = iterator.next();
        assertThat("chunkItem1.data matches expected output String", fromByteArray(dataPartitionerResult1.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo1.id", dataPartitionerResult1.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("dataPartitionerResult.chunkItem matches Failed Record ", iterator.hasNext(), is(true));
        try {
            iterator.next();
            Assertions.fail("Expected error not thrown");
        } catch (InvalidDataException e) {
            assertThat("Expected throwable leading to InvalidDataException", e.getMessage().contains("Cannot read 1239 got:  1222"), is(true));
        }
    }

    @Test
    @Timeout(5)
    public void iso2709DataPartitioner_iteration_terminates() {
        byte[] isoRecords = getResourceAsByteArray(INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO);
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO), "LATIN-1");

        int numberOfIterations = 0;
        for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(4));
        assertThat("dataPartitioner.getBytesRead()", dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test
    @Timeout(5)
    public void iso2709DataPartitioner_drain_items() {
        byte[] isoRecords = getResourceAsByteArray("test-records-74-danmarc2.iso");
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream("test-records-74-danmarc2.iso"), "LATIN-1");

        int numberOfIterations = 14;
        dataPartitioner.drainItems(numberOfIterations);
        for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
            assertThat("result" + numberOfIterations + " position in datafile",
                    dataPartitionerResult.getPositionInDatafile(), is(numberOfIterations++));
        }
        assertThat("Number of iterations", numberOfIterations - 14, is(60));
        assertThat("dataPartitioner.getBytesRead()", dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test
    public void iso2709DataPartitioner_fourRecordsWithErrorInRecordTwo_returnsExpectedDataPartitionerResults() {
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_4_ERROR_IN_RECORD2), "LATIN-1");
        Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat("has 1st result", iterator.hasNext(), is(true));
        DataPartitionerResult result = iterator.next();
        assertThat("1st result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("1st result record id", result.getRecordInfo().getId(), is("x8888888"));
        assertThat("1st result position in datafile", result.getPositionInDatafile(), is(0));

        assertThat("has 2nd record", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("2nd result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd result chunk item diagnostics", result.getChunkItem().getDiagnostics().size(), is(1));
        assertThat("2nd result record info", result.getRecordInfo(), is(nullValue()));
        assertThat("2nd result position in datafile", result.getPositionInDatafile(), is(1));

        assertThat("has 3rd result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("3rd result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("3rd result record id", result.getRecordInfo().getId(), is("9788793128231"));
        assertThat("3rd result position in datafile", result.getPositionInDatafile(), is(2));

        assertThat("has 4th result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("4th result chunk item status", result.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("4th result record id", result.getRecordInfo().getId(), is("9788771185980"));
        assertThat("4th result position in datafile", result.getPositionInDatafile(), is(3));

        assertThat("has 5th result", iterator.hasNext(), is(false));
    }

    @Test
    public void iso2709DataPartitioner_marc21utf8() {
        DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_323_MARC21_UTF8_ISO), "UTF-8");
        Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertThat(count, is(323));
    }

    private static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }
}
