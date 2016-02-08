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
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.parsers.ParserConfigurationException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("No more records => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecord.length, dataPartitioner.getBytesRead(), is((long) isoRecord.length));
    }

    @Test
    public void iso2709DataPartitioner_multipleRecords_accepted() {
        final byte[] isoRecords = getResourceAsByteArray(INPUT_RECORDS_3_ISO);
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_3_ISO), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("Second record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("Third record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("No more records => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecords.length, dataPartitioner.getBytesRead(), is((long) isoRecords.length));

    }

    @Test
    public void iso2709DataPartitioner_invalidIso2709_throws() throws ParserConfigurationException {
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_BROKEN_ISO), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        // 2 good records
        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("Second record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getResourceAsStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("next matches Failed Record ",iterator.hasNext(), is(true) );
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
        for (ChunkItem ci : dataPartitioner) {
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(4));
        assertThat("dataPartitioner.getBytesRead()", dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test
    public void iso2709DataPartitioner_fourRecordsWithErrorInRecordTwo_returnsExpectedChunkItems() {
        final DataPartitioner dataPartitioner = Iso2709DataPartitioner.newInstance(getResourceAsStream(INPUT_RECORDS_4_ERROR_IN_RECORD2), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat("item0.status", iterator.next().getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat("item1.status", item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("item1.diagnostics", item1.getDiagnostics().size(), is(1));

        assertThat(iterator.hasNext(), is(true));
        assertThat("item2.status", iterator.next().getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat(iterator.hasNext(), is(true));
        assertThat("item3.status", iterator.next().getStatus(), is(ChunkItem.Status.SUCCESS));

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
