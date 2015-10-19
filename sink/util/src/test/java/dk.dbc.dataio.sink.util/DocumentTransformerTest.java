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

    private static final String NAMESPACE_URI = "dk.dbc.dataio.processing";
    private static final String ELEMENT_1 = "sink-update-template";
    private static final String ELEMENT_2 = "sink-processing";
    private final DocumentTransformer documentTransformer = new DocumentTransformer();

    @Test
    public void setDocumentTransformer_newInstance_returnsNewDocumentTransformer() {
        DocumentTransformer documentTransformer = new DocumentTransformer();
        assertThat(documentTransformer, not(nullValue()));
    }

    @Test
    public void documentTransformer_callGetDocumentWithValidXml_returnsDocument() throws IOException, SAXException {
        // Subject under test
        Document document = documentTransformer.byteArrayToDocument(getByteArray(getContentXml()));

        // Verification
        assertThat("Document not null", document, not(nullValue()));
    }

    @Test
    public void documentTransformer_callGetDocumentWithInvalidXml_throws() throws IOException {
        try {
            // Subject under test
            documentTransformer.byteArrayToDocument(getByteArray(getInvalidContentXml()));
            fail();
        } catch (Exception e) {
            // Verification
            assertThat(e instanceof SAXParseException, is(true));
        }
    }

    @Test
    public void documentTransformer_fromDocumentToByteArray_returnsByteArray() throws TransformerException {
        final String contentDataAsString = getContentXml();
        final byte[] inputByteArray = getByteArray(contentDataAsString);
        Document document = getDocument(inputByteArray);

        // Subject under test
        byte[] returnedByteArray = documentTransformer.documentToByteArray(document);

        // Verification
        assertThat("Length of byte array", returnedByteArray.length > 0, is(true));
        assertThat("Content contains" + contentDataAsString, new String(returnedByteArray, StandardCharsets.UTF_8).contains(contentDataAsString), is(true));
    }

    @Test
    public void documentTransformer_removeFromDom_removesChildNodesFromDom() {
        final Document metaDataDocument = getDocument(getByteArray(getMetaXml()));
        final NodeList nodeListContainingElementsWithTagName = metaDataDocument.getElementsByTagNameNS(NAMESPACE_URI, ELEMENT_1);
        final NodeList nodeListContainingElementsWithTagExtra = metaDataDocument.getElementsByTagNameNS(NAMESPACE_URI, ELEMENT_2);

        assertThat("Nodelist containing elements with ", nodeListContainingElementsWithTagName.getLength(), is(2));
        assertThat(nodeListContainingElementsWithTagExtra.getLength(), is(1));

        // Subject under test
        documentTransformer.removeFromDom(nodeListContainingElementsWithTagName);

        // Verification
        assertThat(nodeListContainingElementsWithTagName.getLength(), is(0));
        assertThat(nodeListContainingElementsWithTagExtra.getLength(), is(1));
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

    private String getMetaXml() {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" testNodeName=\"testValue\" charset=\"danmarc2\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" testNodeName=\"testValue\" charset=\"danmarc2\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" testNodeName=\"testValue\" charset=\"danmarc2\"/>" +
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
