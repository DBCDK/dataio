package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AddiRecordPreprocessorTest {
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    {
        documentBuilderFactory.setNamespaceAware(true);
    }

    @Test
    public void execute_noProcessingTag_returnsUnchangedMetadataWithUnchangedContent() {
        final AddiRecordPreprocessor preprocessor = new AddiRecordPreprocessor();
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithoutProcessing());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord);
        assertThat("AddiRecord.metadata is unchanged", preprocessed.getMetaData(), is(addiRecord.getMetaData()));
        assertThat("AddiRecord.content is unchanged", preprocessed.getContentData(), is(addiRecord.getContentData()));
    }

    @Test
    public void execute_processingTagWithEncodeAs2709SetToFalse_returnsUpdatedMetadataWithUnchangedContent() {
        final AddiRecordPreprocessor preprocessor = new AddiRecordPreprocessor();
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithProcessingFalse());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord);
        assertThat("AddiRecord.metadata has processing tag", hasProcessingTag(preprocessed), is(false));
        assertThat("AddiRecord.content is unchanged", preprocessed.getContentData(), is(addiRecord.getContentData()));
    }

    @Test
    public void execute_processingTagWithEncodeAs2709SetToTrue_returnsUpdatedMetadataWithUpdatedContent() {
        final AddiRecordPreprocessor preprocessor = new AddiRecordPreprocessor();
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithProcessingTrueAndValidMarcXContentData());
        final AddiRecord preprocessed = preprocessor.execute(addiRecord);
        assertThat("AddiRecord.metadata has processing tag", hasProcessingTag(preprocessed), is(false));
        assertThat("AddiRecord.content is 2709 encoded", preprocessed.getContentData(), is(to2709(addiRecord.getContentData())));
    }

    @Test
    public void execute_processingTagWithEncodeAs2709SetToTrueAndInvalidRecordContent_throws() {
        final AddiRecordPreprocessor preprocessor = new AddiRecordPreprocessor();
        final AddiRecord addiRecord = toAddiRecord(getValidAddiWithProcessingTrueAndInvalidMarcXContentData());
        try {
            preprocessor.execute(addiRecord);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    private boolean hasProcessingTag(AddiRecord addiRecord) {
        final Document metadata = getDocument(addiRecord.getMetaData());
        return metadata.getElementsByTagName(AddiRecordPreprocessor.PROCESSING_TAG).getLength() > 0;
    }

    private Document getDocument(byte[] byteArray) {
        final DocumentBuilder builder = getDocumentBuilder();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        try {
            return builder.parse(byteArrayInputStream);
        } catch (SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private DocumentBuilder getDocumentBuilder() {
        try {
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] to2709(byte[] document) {
        try {
            return Iso2709Packer.create2709FromMarcXChangeRecord(getDocument(document), new DanMarc2Charset());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] getValidAddiWithoutProcessing() {
        return ("131\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "</es:referencedata>\n1\nb\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getValidAddiWithProcessingFalse() {
        return ("236\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"false\" charset=\"danmarc2\"/>" +
                "</es:referencedata>\n1\nb\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getValidAddiWithProcessingTrueAndValidMarcXContentData() {
        return ("235\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>" +
                "\n506\n" +
                "<marcx:record xmlns:marcx='info:lc/xmlns/marcxchange-v1'>" +
                "<marcx:leader>00000n    2200000   4500</marcx:leader>" +
                "<marcx:datafield tag='100' ind1='0' ind2='0'>" +
                "<marcx:subfield code='a'>field1</marcx:subfield>" +
                "<marcx:subfield code='b'/>" +
                "<marcx:subfield code='d'>Field2</marcx:subfield>" +
                "</marcx:datafield><marcx:datafield tag='101' ind1='1' ind2='2'>" +
                "<marcx:subfield code='h'>est</marcx:subfield>" +
                "<marcx:subfield code='k'>o</marcx:subfield>" +
                "<marcx:subfield code='G'>ris</marcx:subfield>" +
                "</marcx:datafield></marcx:record>\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getValidAddiWithProcessingTrueAndInvalidMarcXContentData() {
        return ("235\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>" +
                "\n238\n" +
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
                "</marcx:record>" +
                "</marcx:collection>" +
                "\n").getBytes(StandardCharsets.UTF_8);
    }

}