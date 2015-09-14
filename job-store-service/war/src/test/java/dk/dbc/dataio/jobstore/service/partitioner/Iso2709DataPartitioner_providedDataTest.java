package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.junit.Test;

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
import static org.xmlmatchers.XmlMatchers.isEquivalentTo;
import static org.xmlmatchers.transform.XmlConverters.the;

public class Iso2709DataPartitioner_providedDataTest {

    private final static String SPECIFIED_ENCODING = "latin1";

    private final static String INPUT_RECORD_1_ISO = "test-record-1-danmarc2.iso";
    private final static String INPUT_RECORDS_3_ISO = "test-records-3-danmarc2.iso";
    private final static String OUTPUT_RECORD_1_MARCXCHANGE = "test-record-1-danmarc2.marcXChange";

    @Test
    public void specifiedEncodingDiffersFromActualEncoding_throws() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), "latin 1");
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) { }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncoding_throws() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingInLowerCase_throws() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), "LATIN1");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterTrim_throws() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), " latin1 ");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterDashReplace_throws() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), "latin-1");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void getEncoding_expectedEncodingReturned() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory().
                createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        assertThat("Encoding", dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void iso2709DataPartitioner_oneValidRecord_accepted() throws IOException, URISyntaxException {

        final byte[] isoRecord = readTestRecord(INPUT_RECORD_1_ISO);
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(new ByteArrayInputStream(isoRecord), SPECIFIED_ENCODING);
        final Iterator<String> iterator = dataPartitioner.iterator();

        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", the(iterator.next()), isEquivalentTo(the(getMarcXChangeOutputRecordAsString())));

        assertThat("No more records => hasNext() expected to be false", iterator.hasNext(), is(false));

        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecord.length, dataPartitioner.getBytesRead(), is((long) isoRecord.length));
    }

    @Test
    public void iso2709DataPartitioner_multipleRecords_accepted() throws IOException, URISyntaxException {
        final byte[] isoRecords = readTestRecord(INPUT_RECORDS_3_ISO);
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(new ByteArrayInputStream(isoRecords), SPECIFIED_ENCODING);
        final Iterator<String> iterator = dataPartitioner.iterator();

        assertThat("First record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", the(iterator.next()), isEquivalentTo(the(getMarcXChangeOutputRecordAsString())));

        assertThat("Second record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", the(iterator.next()), isEquivalentTo(the(getMarcXChangeOutputRecordAsString())));

        assertThat("Third record => hasNext() expected to be true", iterator.hasNext(), is(true));
        assertThat("next matches expected output String", the(iterator.next()), isEquivalentTo(the(getMarcXChangeOutputRecordAsString())));

        assertThat("No more records => hasNext() expected to be false", iterator.hasNext(), is(false));

        assertThat("dataPartitioner.getBytesRead(): " + dataPartitioner.getBytesRead() + ", is expected to match: " + isoRecords.length, dataPartitioner.getBytesRead(), is((long) isoRecords.length));
    }

    @Test
    public void iso2709DataPartitioner_emptyInputStream_accepted() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(new ByteArrayInputStream(new byte[0]), SPECIFIED_ENCODING);
        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat("No records => hasNext() expected to be false", iterator.hasNext(), is(false));
    }

    /*
     * Package private methods
     */

    static InputStream asByteArrayInputStream(String resourceName) throws IOException, URISyntaxException {
        return new ByteArrayInputStream(readTestRecord(resourceName));
    }

    static String getMarcXChangeOutputRecordAsString() throws IOException, URISyntaxException {
        return new String(readTestRecord(OUTPUT_RECORD_1_MARCXCHANGE), StandardCharsets.UTF_8);
    }

    static byte[] readTestRecord(String resourceName) throws IOException, URISyntaxException {
        final URL url = Iso2709DataPartitioner_providedDataTest.class.getResource("/" + resourceName);
        final Path resPath;
        resPath = Paths.get(url.toURI());
        return Files.readAllBytes(resPath);
    }

}
