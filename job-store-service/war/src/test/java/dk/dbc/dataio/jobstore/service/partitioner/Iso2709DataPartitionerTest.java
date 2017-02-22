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
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.parsers.ParserConfigurationException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.xmlunit.builder.Input.fromByteArray;
import static org.xmlunit.builder.Input.fromStream;

public class Iso2709DataPartitionerTest extends AbstractPartitionerTestBase{

    private final static String SPECIFIED_ENCODING = "latin1";

    private final static String INPUT_RECORD_1_ISO = "/test-record-1-danmarc2.iso";
    private final static String INPUT_BROKEN_ISO = "/broken-iso2709-2.iso";
    private final static String INPUT_RECORDS_3_ISO = "/test-records-3-danmarc2.iso";
    private final static String INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO = "/test-records-4-danmarc2-guard-against-infinite-iteration.iso";
    private final static String INPUT_RECORDS_4_ERROR_IN_RECORD2 = "/test-records-4-error-in-record2.iso";
    private final static String OUTPUT_RECORD_1_MARCXCHANGE = "/test-record-1-danmarc2.marcXChange";
    private final static String DEFAULT_RECORD_ID = "30769430";

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        try {
            Iso2709DataPartitioner.newInstance(getEmptyInputStream(), null);
            fail("No exception thrown");
        } catch (NullPointerException e) { }
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        try {
            Iso2709DataPartitioner.newInstance(getEmptyInputStream(), "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) { }
    }

    @Test
    public void newInstance_specifiedEncodingDiffersFromActualEncoding_throws() {
        try {
            Iso2709DataPartitioner.newInstance(getEmptyInputStream(), "latin 1");
            fail("No exception thrown");
        } catch (InvalidEncodingException e) { }
    }

    @Test
    public void newInstance_specifiedEncodingIdenticalToActualEncoding_returnsNewDataPartitioner() {
        assertThat(Iso2709DataPartitioner.newInstance(getEmptyInputStream(), SPECIFIED_ENCODING), is(notNullValue()));
    }

    @Test
    public void newInstance_specifiedEncodingIdenticalToActualEncodingInLowerCase_returnsNewDataPartitioner() {
        assertThat(Iso2709DataPartitioner.newInstance(getEmptyInputStream(), "LATIN1"), is(notNullValue()));
    }

    @Test
    public void newInstance_specifiedEncodingIdenticalToActualEncodingAfterTrim_returnsNewDataPartitioner() {
        assertThat(Iso2709DataPartitioner.newInstance(getEmptyInputStream(), " latin1 "), is(notNullValue()));
    }

    @Test
    public void newInstance_specifiedEncodingIdenticalToActualEncodingAfterDashReplace_returnsNewDataPartitioner() {
        assertThat(Iso2709DataPartitioner.newInstance(getEmptyInputStream(), "latin-1"), is(notNullValue()));
    }

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            Iso2709DataPartitioner.newInstance(null, SPECIFIED_ENCODING);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_allArgsAreValid_returnsNewDataPartitioner() {
        assertThat(Iso2709DataPartitioner.newInstance(getEmptyInputStream(), SPECIFIED_ENCODING), is(notNullValue()));
    }

    @Test
    public void getEncoding_expectedEncodingReturned() {
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getEmptyInputStream(), SPECIFIED_ENCODING);
        assertThat("Encoding", dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void iso2709DataPartitioner_oneValidRecord_accepted() {
        final byte[] isoRecord = getResourceAsByteArray(INPUT_RECORD_1_ISO);
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat("First dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat("chunkItem0.data matches expected output String", fromByteArray(dataPartitionerResult.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo1.id", dataPartitionerResult.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));
        assertThat("No more dataPartitionerResults => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecord.length, dataPartitioner.getBytesRead(), is((long) isoRecord.length));
    }

    @Test
    public void iso2709DataPartitioner_multipleRecords_accepted() {
        final byte[] isoRecords = getResourceAsByteArray(INPUT_RECORDS_3_ISO);
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_3_ISO), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat("First dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult0 = iterator.next();
        assertThat("chunkItem0.data matches expected output String", fromByteArray(dataPartitionerResult0.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo0.id", dataPartitionerResult0.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("Second dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult1 = iterator.next();
        assertThat("chunkItem1 matches expected output String", fromByteArray(dataPartitionerResult1.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo1.id", dataPartitionerResult1.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("Third record => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult2 = iterator.next();
        assertThat("chunkItem2.data matches expected output String", fromByteArray(dataPartitionerResult2.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo2.id", dataPartitionerResult2.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("No more dataPartitionerResults => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecords.length, dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test
    public void iso2709DataPartitioner_invalidIso2709_throws() throws ParserConfigurationException {
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_BROKEN_ISO), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        // 2 good records
        assertThat("First dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult0 = iterator.next();
        assertThat("chunkItem0.data matches expected output String", fromByteArray(dataPartitionerResult0.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo0.id", dataPartitionerResult0.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("Second dataPartitionerResult => hasNext() expected to be true", iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult1 = iterator.next();
        assertThat("chunkItem1.data matches expected output String", fromByteArray(dataPartitionerResult1.getChunkItem().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("recordInfo1.id", dataPartitionerResult1.getRecordInfo().getId(), is(DEFAULT_RECORD_ID));

        assertThat("dataPartitionerResult.chunkItem matches Failed Record ",iterator.hasNext(), is(true) );
        try {
            iterator.next();
            fail("Expected error not thrown");
        } catch (InvalidDataException e) {
            assertThat("Expected throwable leading to InvalidDataException", e.getMessage().contains("Cannot read 1239 got:  1222"), is(true));
        }
    }

    @Test(timeout = 5000)
    public void iso2709DataPartitioner_iteration_terminates() {
        final byte[] isoRecords = getResourceAsByteArray(INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO);
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO), SPECIFIED_ENCODING);

        int numberOfIterations = 0;
        for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(4));
        assertThat("dataPartitioner.getBytesRead()", dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test(timeout = 5000)
    public void iso2709DataPartitioner_drain_items() {
        final byte[] isoRecords = getResourceAsByteArray("/test-records-74-danmarc2.iso");
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream("/test-records-74-danmarc2.iso"), SPECIFIED_ENCODING);

        int numberOfIterations = 14;
        dataPartitioner.drainItems( numberOfIterations );
        for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(74));
        assertThat("dataPartitioner.getBytesRead()", dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }


    @Test
    public void iso2709DataPartitioner_fourRecordsWithErrorInRecordTwo_returnsExpectedDataPartitionerResults() {
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_4_ERROR_IN_RECORD2), SPECIFIED_ENCODING);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult0 = iterator.next();
        assertThat("item0.status", dataPartitionerResult0.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        final RecordInfo recordInfo0 = dataPartitionerResult0.getRecordInfo();
        assertThat(recordInfo0.getId(), is("x8888888"));

        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult1 = iterator.next();
        final ChunkItem item1 = dataPartitionerResult1.getChunkItem();
        assertThat("item1.status", item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("item1.diagnostics", item1.getDiagnostics().size(), is(1));
        assertThat("recordInfo1", dataPartitionerResult1.getRecordInfo(), is(nullValue()));

        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult2 = iterator.next();
        assertThat("item2.status", dataPartitionerResult2.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        final RecordInfo recordInfo2 = dataPartitionerResult2.getRecordInfo();
        assertThat("recordInfo2.id",recordInfo2.getId(), is("9788793128231"));

        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult3 = iterator.next();
        assertThat("item3.status", dataPartitionerResult3.getChunkItem().getStatus(), is(ChunkItem.Status.SUCCESS));
        final RecordInfo recordInfo3 = dataPartitionerResult3.getRecordInfo();
        assertThat(recordInfo3.getId(), is("9788771185980"));

        assertThat(iterator.hasNext(), is(false));
    }

    /*
     * Package private methods
     */

    private static CompareMatcher isEquivalentTo( Object control) {
         return CompareMatcher.isSimilarTo(control)
                 .throwComparisonFailure()
                 .normalizeWhitespace()
                 .ignoreComments();
    }
}
