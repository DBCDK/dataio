package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.marc.Iso2709Unpacker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.xmlmatchers.XmlMatchers.isEquivalentTo;
import static org.xmlmatchers.transform.XmlConverters.the;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Iso2709Unpacker.class,
        TransformerFactory.class})
public class Iso2709DataPartitionerTest {

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

    @Test
    public void iso2709DataPartitioner_invalidIso2709_throws() throws ParserConfigurationException, IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        final Iterator<String> iterator = dataPartitioner.iterator();

        mockStatic(Iso2709Unpacker.class);
        when(Iso2709Unpacker.createMarcXChangeRecord(
                any(BufferedInputStream.class),
                any(Charset.class),
                any(DocumentBuilderFactory.class)))
                .thenThrow(new IOException("Error msg"));

        try {
            iterator.hasNext();
            fail("Expected error not thrown");
        } catch (InvalidDataException e) {
            assertThat("Expected throwable leading to InvalidDataException", e.getCause() instanceof IOException, is(true));
        }
    }

    @Test
    public void iso2709DataPartitioner_errorConvertingDocumentToString_throws() throws IOException, URISyntaxException, TransformerException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new Iso2709DataPartitionerFactory()
                .createDataPartitioner(asByteArrayInputStream(INPUT_RECORD_1_ISO), SPECIFIED_ENCODING);
        final Iterator<String> iterator = dataPartitioner.iterator();

        mockStatic(TransformerFactory.class);
        TransformerFactory mockedTransformerFactory = mock(TransformerFactory.class);
        Transformer mockedTransformer = mock(Transformer.class);

        when(TransformerFactory.newInstance()).thenReturn(mockedTransformerFactory);
        when(mockedTransformerFactory.newTransformer()).thenReturn(mockedTransformer);
        doThrow(new TransformerException("Error msg")).when(mockedTransformer).transform(any(Source.class), any(Result.class));

        iterator.hasNext();
        try {
            iterator.next();
            fail("Expected error not thrown");
        } catch (InvalidDataException e) {
            assertThat("Expected throwable leading to InvalidDataException", e.getCause() instanceof TransformerException, is(true));
        }
    }

    private InputStream asByteArrayInputStream(String resourceName) throws IOException, URISyntaxException {
        return new ByteArrayInputStream(readTestRecord(resourceName));
    }

    private String getMarcXChangeOutputRecordAsString() throws IOException, URISyntaxException {
        return new String(readTestRecord(OUTPUT_RECORD_1_MARCXCHANGE), StandardCharsets.UTF_8);
    }

    private static byte[] readTestRecord(String resourceName) throws IOException, URISyntaxException {
        final URL url = Iso2709DataPartitionerTest.class.getResource("/" + resourceName);
        final java.nio.file.Path resPath;
        resPath = java.nio.file.Paths.get(url.toURI());
        return java.nio.file.Files.readAllBytes(resPath);
    }

}
