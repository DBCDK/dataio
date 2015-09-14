package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidDataException;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Iterator;

import static dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitioner_dataTest.asByteArrayInputStream;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Iso2709Unpacker.class,
        TransformerFactory.class})
public class Iso2709DataPartitioner_mockedDataTest {

    private final static String SPECIFIED_ENCODING = "latin1";
    private final static String INPUT_RECORD_1_ISO = "test-record-1-danmarc2.iso";

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
}
