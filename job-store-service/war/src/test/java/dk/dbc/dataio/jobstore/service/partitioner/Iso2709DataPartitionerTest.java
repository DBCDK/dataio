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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.xmlunit.builder.Input.fromByteArray;
import static org.xmlunit.builder.Input.fromStream;

public class Iso2709DataPartitionerTest {

    private final static String SPECIFIED_ENCODING = "latin1";

    private final static String INPUT_RECORD_1_ISO = "/test-record-1-danmarc2.iso";
    private final static String INPUT_BROKEN_ISO = "/broken-iso2709-2.iso";
    private final static String INPUT_RECORDS_3_ISO = "/test-records-3-danmarc2.iso";
    private final static String INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO = "/test-records-4-danmarc2-guard-against-infinite-iteration.iso";
    private final static String OUTPUT_RECORD_1_MARCXCHANGE = "/test-record-1-danmarc2.marcXChange";

    @Test
    public void specifiedEncodingDiffersFromActualEncoding_throws() {
        try {
            new Iso2709DataPartitionerFactory().createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), "latin 1");
            fail("No exception thrown");
        } catch (InvalidEncodingException e) { }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncoding_instanceCreated() {
        new Iso2709DataPartitionerFactory().createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingInLowerCase_instanceCreated() {
        new Iso2709DataPartitionerFactory().createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), "LATIN1");
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterTrim_instanceCreated() {
        new Iso2709DataPartitionerFactory().createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), " latin1 ");
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterDashReplace_instanceCreated() {
        new Iso2709DataPartitionerFactory().createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), "latin-1");
    }

    @Test
    public void getEncoding_expectedEncodingReturned() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        assertThat("Encoding", dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void iso2709DataPartitioner_oneValidRecord_accepted() {
        final byte[] isoRecord = readTestRecord(INPUT_RECORD_1_ISO);
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(getTestInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getTestInputStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("No more records => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecord.length, dataPartitioner.getBytesRead(), is((long) isoRecord.length));
    }

    @Test
    public void iso2709DataPartitioner_multipleRecords_accepted() {
        final byte[] isoRecords = readTestRecord(INPUT_RECORDS_3_ISO);
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(new ByteArrayInputStream(isoRecords), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getTestInputStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("Second record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getTestInputStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("Third record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getTestInputStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("No more records => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecords.length, dataPartitioner.getBytesRead(), is((long) isoRecords.length));

    }

    @Test
    public void iso2709DataPartitioner_emptyInputStream_accepted() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(new ByteArrayInputStream(new byte[0]), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat("No records => hasNext() expected to be false", iterator.hasNext(), is(false));
    }


    @Test
    public void iso2709DataPartitioner_invalidIso2709_throws() throws ParserConfigurationException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(getTestInputStream(INPUT_BROKEN_ISO), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        // 2 good records
        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getTestInputStream(OUTPUT_RECORD_1_MARCXCHANGE))));
        assertThat("Second record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", fromByteArray(iterator.next().getData()), isEquivalentTo(fromStream(getTestInputStream(OUTPUT_RECORD_1_MARCXCHANGE))));

        try {
            iterator.hasNext();
            fail("Expected error not thrown");
        } catch (InvalidDataException e) {
            assertThat("Expected throwable leading to InvalidDataException", e.getCause() instanceof IOException, is(true));
        }
    }

    @Test
    public void iso2709DataPartitioner_iteration_terminates() {
        final byte[] isoRecords = readTestRecord(INPUT_RECORDS_4_GUARD_AGAINST_INFINITE_ITERATION_ISO);
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(new ByteArrayInputStream(isoRecords), SPECIFIED_ENCODING);

        int numberOfIterations = 0;
        for (ChunkItem ci : dataPartitioner) {
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(4));
        assertThat("dataPartitioner.getBytesRead()", dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    /*
     * Package private methods
     */

    static InputStream getTestInputStream( String resourceName ) {
        return Iso2709DataPartitionerTest.class.getResourceAsStream(resourceName);
    }

    public static CompareMatcher isEquivalentTo( Object control) {
         return CompareMatcher.isSimilarTo(control)
                 .throwComparisonFailure()
                 .normalizeWhitespace()
                 .ignoreComments();
    }

    private static byte[] readTestRecord(String resourceName) {
        try {
            final URL url = Iso2709DataPartitionerTest.class.getResource(resourceName);
            final Path resPath;
            resPath = Paths.get(url.toURI());
            return Files.readAllBytes(resPath);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
