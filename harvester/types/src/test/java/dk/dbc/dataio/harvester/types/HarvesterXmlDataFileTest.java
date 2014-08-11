package dk.dbc.dataio.harvester.types;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class HarvesterXmlDataFileTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private final Charset charset = StandardCharsets.UTF_8;
    private final OutputStream outputStream = mock(OutputStream.class);

    @Test(expected = NullPointerException.class)
    public void constructor_charsetArgIsNull_throws() throws HarvesterException {
        new HarvesterXmlDataFile(null, outputStream);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_outputStreamArgIsNull_throws() throws HarvesterException {
        new HarvesterXmlDataFile(charset, null);
    }

    @Test(expected = HarvesterException.class)
    public void constructor_writingOfHeaderThrowsIOException_throws() throws HarvesterException, IOException {
        doThrow(new IOException()).when(outputStream).write(any(byte[].class), anyInt(), anyInt());
        new HarvesterXmlDataFile(charset, outputStream);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() throws HarvesterException {
        final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(charset, outputStream);
        assertThat(harvesterDataFile, is(notNullValue()));
    }

    @Test
    public void addRecord_recordArgIsNull_throws() throws HarvesterException {
        final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(charset, outputStream);
        try {
            harvesterDataFile.addRecord(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addRecord_charsetMismatch_throws() throws HarvesterException {
        final MockedHarvesterXmlRecord harvesterRecord = new MockedHarvesterXmlRecord();
        harvesterRecord.setCharset(StandardCharsets.ISO_8859_1);
        final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(charset, outputStream);
        try {
            harvesterDataFile.addRecord(harvesterRecord);
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addRecord_writingOfRecordDataThrowsIOException_throws() throws HarvesterException, IOException {
        final MockedHarvesterXmlRecord harvesterRecord = new MockedHarvesterXmlRecord();
        harvesterRecord.setCharset(StandardCharsets.UTF_8);
        harvesterRecord.setData("data".getBytes(StandardCharsets.UTF_8));
        final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(charset, outputStream);
        try {
            doThrow(new IOException()).when(outputStream).write(any(byte[].class), anyInt(), anyInt());
            harvesterDataFile.close();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void close_writingOfFooterThrowsIOException_throws() throws HarvesterException, IOException {
        final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(charset, outputStream);
        try {
            doThrow(new IOException()).when(outputStream).write(any(byte[].class), anyInt(), anyInt());
            harvesterDataFile.close();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void isAutoClosable() throws HarvesterException {
        try (final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(charset, outputStream)) {
            assertThat(harvesterDataFile, is(notNullValue()));
        }
    }

    @Test
    public void writesXml() throws HarvesterException, IOException, ParserConfigurationException, SAXException {
        final File outputFile = testFolder.newFile();
        final HarvesterXmlDataFile harvesterDataFile =
                new HarvesterXmlDataFile(charset, getOutputStreamForFile(outputFile));
        harvesterDataFile.close();
        parseXmlFile(outputFile);
    }

    private OutputStream getOutputStreamForFile(File file) throws IOException {
        return new FileOutputStream(file);
    }

    private void parseXmlFile(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    documentBuilder.parse(xmlFile);
    }

    private static class MockedHarvesterXmlRecord implements HarvesterXmlRecord {
        byte[] data;
        Charset charset;

        public void setData(byte[] data) {
            this.data = data;
        }

        public void setCharset(Charset charset) {
            this.charset = charset;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public Charset getCharset() {
            return charset;
        }
    }
}