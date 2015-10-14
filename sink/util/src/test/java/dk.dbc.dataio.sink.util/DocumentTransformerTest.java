package dk.dbc.dataio.sink.util;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

public class DocumentTransformerTest {

    private static final String TAG_NAME = "dataio:sink-tag-test";
    private static final String TAG_EXTRA = "dataio:sink-tag-extra";
    private static final String NODE_NAME = "testNodeName";
    private static final String NODE_VALUE = "testValue";

    @Test
    public void documentTransformer_callGetDocumentWithValidXml_returnsDocument() throws IOException, SAXException {
        // Subject under test
        Document document = DocumentTransformer.byteArrayToDocument(getByteArray(getContentXml()));

        // Verification
        assertThat(document, not(nullValue()));
    }

    @Test
    public void documentTransformer_callGetDocumentWithInvalidXml_throws() throws IOException {
        try {
            // Subject under test
            DocumentTransformer.byteArrayToDocument(getByteArray(getInvalidContentXml()));
            fail();
        } catch (Exception e) {
            // Verification
            assertThat(e instanceof SAXParseException, is(true));
        }
    }

    @Test
    public void documentTransformer_callGetNodeValueByExistingName_returnsNodeValue() throws IOException, SAXException {
        final Document metaDataDocument = getDocument(getByteArray(getMetaXml()));
        final NodeList nodeList = metaDataDocument.getElementsByTagName(TAG_NAME);

        // Subject under test
        final String nodeValue = DocumentTransformer.getNodeValue(NODE_NAME, nodeList.item(0));

        // Verification
        assertThat(nodeValue, is(NODE_VALUE));
    }

    @Test
    public void documentTransformer_callGetNodeValueByNoneExistingName_returnsNull() throws IOException, SAXException {
        final Document metaDataDocument = getDocument(getByteArray(getMetaXml()));
        final NodeList nodeList = metaDataDocument.getElementsByTagName(TAG_NAME);

        // Subject under test
        final String nodeValue = DocumentTransformer.getNodeValue("fisk", nodeList.item(0));

        // Verification
        assertThat(nodeValue, is(nullValue()));
    }

    @Test
    public void documentTransformer_fromDocumentToByteArray_returnsByteArray() throws TransformerException {
        final String contentDataAsString = getContentXml();
        final byte[] inputByteArray = getByteArray(contentDataAsString);
        Document document = getDocument(inputByteArray);

        // Subject under test
        byte[] returnedByteArray = DocumentTransformer.documentToByteArray(document);

        // Verification
        assertThat(returnedByteArray.length > 0, is(true));
        assertThat(new String(returnedByteArray, StandardCharsets.UTF_8).contains(contentDataAsString), is(true));
    }

    @Test
    public void documentTransformer_removeFromDom_removesChildNodesFromDom() {
        final Document metaDataDocument = getDocument(getByteArray(getMetaXml()));
        final NodeList nodeListContainingElementsWithTagName = metaDataDocument.getElementsByTagName(TAG_NAME);
        final NodeList nodeListContainingElementsWithTagExtra = metaDataDocument.getElementsByTagName(TAG_EXTRA);

        assertThat(nodeListContainingElementsWithTagName.getLength(), is(2));
        assertThat(nodeListContainingElementsWithTagExtra.getLength(), is(1));

        // Subject under test
        DocumentTransformer.removeFromDom(nodeListContainingElementsWithTagName);

        // Verification
        assertThat(nodeListContainingElementsWithTagName.getLength(), is(0));
        assertThat(nodeListContainingElementsWithTagExtra.getLength(), is(1));
    }

    private Document getDocument(byte[] bytes) {
        try {
            return DocumentTransformer.byteArrayToDocument(bytes);
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private byte[] getByteArray(String input) {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    private String getMetaXml() {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-tag-test xmlns:dataio=\"dk.dbc.dataio.processing\" testNodeName=\"testValue\" charset=\"danmarc2\"/>" +
                "<dataio:sink-tag-test xmlns:dataio=\"dk.dbc.dataio.processing\" testNodeName=\"testValue\" charset=\"danmarc2\"/>" +
                "<dataio:sink-tag-extra xmlns:dataio=\"dk.dbc.dataio.processing\" testNodeName=\"testValue\" charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    private String getContentXml() {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">field1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    private String getInvalidContentXml() {
        return  "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }
}
