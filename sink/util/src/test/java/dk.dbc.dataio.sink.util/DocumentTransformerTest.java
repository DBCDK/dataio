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
    private static final String NAMESPACE_URI = "ns";
    private static final String ELEMENT = "element";
    private static final String ATTRIBUTE = "attribute";
    private static final String ATTRIBUTE_VALUE = "attributeValue";
    private final DocumentTransformer documentTransformer = new DocumentTransformer();

    @Test
    public void setDocumentTransformer_newInstance_returnsNewDocumentTransformer() {
        assertThat(new DocumentTransformer(), not(nullValue()));
    }

    @Test
    public void documentTransformer_callGetDocumentWithValidXml_returnsDocument() throws IOException, SAXException {
        // Subject under test
        final Document document = documentTransformer.byteArrayToDocument(getByteArray(getXmlSingleElement()));

        // Verification
        assertThat("Document not null", document, not(nullValue()));
    }

    @Test
    public void documentTransformer_callGetDocumentWithInvalidXml_throws() throws IOException {
        try {
            // Subject under test
            documentTransformer.byteArrayToDocument(getByteArray("invalid xml"));
            fail();
        } catch (Exception e) {
            // Verification
            assertThat(e instanceof SAXParseException, is(true));
        }
    }

    @Test
    public void documentTransformer_fromDocumentToByteArray_returnsByteArray() throws TransformerException {
        final String xmlString = getXmlSingleElement();
        final Document document = getDocument(getByteArray(xmlString));

        // Subject under test
        byte[] returnedByteArray = documentTransformer.documentToByteArray(document);

        // Verification
        assertThat("Length of byte array", returnedByteArray.length > 0, is(true));
        assertThat("Document contains " + xmlString, new String(returnedByteArray, StandardCharsets.UTF_8).contains(xmlString), is(true));
    }

    @Test
    public void documentTransformer_removeFromDom_removesChildNodesFromDom() {
        final Document document = getDocument(getByteArray(getXmlMultipleElements()));
        final NodeList nodeListBeforeRemove = document.getElementsByTagNameNS(NAMESPACE_URI, ELEMENT);
        assertThat("Number of elements before remove", nodeListBeforeRemove.getLength(), is(2));

        // Subject under test
        documentTransformer.removeFromDom(nodeListBeforeRemove);

        // Verification
        final NodeList nodeListAfterRemove = document.getElementsByTagNameNS(NAMESPACE_URI, ELEMENT);
        assertThat("Number of elements after remove", nodeListAfterRemove.getLength(), is(0));
    }

    @Test
    public void extractAttributeValue_attributeFound_returnsValue() {
        final Document document = getDocument(getByteArray(getXmlSingleElement()));
        assertThat(documentTransformer.extractAttributeValue(document, NAMESPACE_URI, ELEMENT, ATTRIBUTE), is(ATTRIBUTE_VALUE));
    }

    @Test
    public void extractAttributeValue_elementNotFound_throws() {
        final Document document = getDocument(getByteArray(getXmlSingleElement()));
        try {
            documentTransformer.extractAttributeValue(document, NAMESPACE_URI, "no-such-element", ATTRIBUTE);
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void extractAttributeValue_multipleElementsFound_throws() {
        final Document document = getDocument(getByteArray(getXmlMultipleElements()));
        try {
            documentTransformer.extractAttributeValue(document, NAMESPACE_URI, ELEMENT, ATTRIBUTE);
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    private Document getDocument(byte[] bytes) {
        try {
            return documentTransformer.byteArrayToDocument(bytes);
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private byte[] getByteArray(String input) {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    private String getXmlSingleElement() {
        return "<ns:root xmlns:ns=\"ns\">" +
                "<ns:element attribute=\"attributeValue\"/>" +
                "</ns:root>";
    }

    private String getXmlMultipleElements() {
        return "<ns:root xmlns:ns=\"ns\">" +
                "<ns:element attribute=\"attributeValue\"/>" +
                "<ns:element attribute=\"attributeValue\"/>" +
                "</ns:root>";
    }
}
