package dk.dbc.dataio.harvester.types;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MarcExchangeCollectionTest {
    private final String marcxCollectionSingleRecord =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
              "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
            "</marcx:collection>";

    private final String marcxCollectionMultipleRecords =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
              "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
              "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title2</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
            "</marcx:collection>";

    private final String marcxRecord =
            "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\">" +
              "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\"><marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
            "</marcx:record>";

    private final String marcxRecordNonUtf8Encoding =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
            "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\">" +
              "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\"><marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
            "</marcx:record>";

    private final String marcxCollectionInvalidNamespace =
            "<not-marcx:collection xmlns:not-marcx=\"info:lc/xmlns/not-marcxchange-v1\">" +
              "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\"><marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
            "</not-marcx:collection>";

    private final String marcxCollectionInvalidMemberNamespace =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/not-marcxchange-v1\">" +
              "<invalid:record xmlns:invalid=\"info:lc/xmlns/marcxchange-v2\" format=\"danMARC2\">" +
                "<invalid:datafield ind1=\"0\" ind2=\"0\" tag=\"245\"><invalid:subfield code=\"a\">title1</invalid:subfield></invalid:datafield>" +
              "</invalid:record>" +
            "</marcx:collection>";

   private final String marcxCollectionSingleRecordDefaultNamespace =
            "<collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
              "<record format=\"danMARC2\">" +
                    "<datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                        "<subfield code=\"a\">title1</subfield>" +
                    "</datafield>" +
              "</record>" +
            "</collection>";

    @Test(expected = NullPointerException.class)
    public void constructor_documentBuilderArgIsNull_throws() {
        new MarcExchangeCollection(null, getTransformer());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_transformerArgIsNull_throws() {
        new MarcExchangeCollection(getDocumentBuilder(), null);
    }

    @Test
    public void addMember_memberDataArgIsNull_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember((byte[]) null);
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIsNotValidXml_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember("invalid-xml".getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgDoesNotIndicateUtf8Encoding_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxRecordNonUtf8Encoding.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIncorrectNamespace_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxCollectionInvalidNamespace.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIncorrectChildNamespace_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxCollectionInvalidMemberNamespace.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIsCollectionWithMultipleRecords_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxCollectionMultipleRecords.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIsMarcxCollectionWithSingleRecord_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        harvesterRecord.addMember(marcxCollectionSingleRecord.getBytes());
        assertMarcExchangeCollection(harvesterRecord.asBytes(), 1);
    }

    @Test
    public void addMember_memberDataArgIsMarcxCollectionWithSingleRecordAndDefaultNamespace_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        harvesterRecord.addMember(marcxCollectionSingleRecordDefaultNamespace.getBytes());
        assertMarcExchangeCollection(harvesterRecord.asBytes(), 1);
    }

    @Test
    public void addMember_memberDataArgIsMarcxRecord_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        harvesterRecord.addMember(marcxRecord.getBytes());
        assertMarcExchangeCollection(harvesterRecord.asBytes(), 1);
    }

    @Test
    public void addMember_calledMultipleTimes_multipleRecordsInCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        harvesterRecord.addMember(marcxCollectionSingleRecord.getBytes());
        harvesterRecord.addMember(asDocument(marcxRecord.getBytes()));
        assertMarcExchangeCollection(harvesterRecord.asBytes(), 2);
    }

    @Test
    public void addMember_memberDocArgIsNull_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember((Document) null);
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDocArgDoesNotIndicateUtf8Encoding_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(asDocument(marcxRecordNonUtf8Encoding.getBytes()));
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDocArgIncorrectNamespace_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(asDocument(marcxCollectionInvalidNamespace.getBytes()));
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDocArgIncorrectChildNamespace_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(asDocument(marcxCollectionInvalidMemberNamespace.getBytes()));
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDocArgIsCollectionWithMultipleRecords_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.addMember(asDocument(marcxCollectionMultipleRecords.getBytes()));
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDocArgIsMarcxCollectionWithSingleRecord_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        harvesterRecord.addMember(asDocument(marcxCollectionSingleRecord.getBytes()));
        assertMarcExchangeCollection(harvesterRecord.asBytes(), 1);
    }

    @Test
    public void addMember_memberDocArgIsMarcxRecord_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        harvesterRecord.addMember(asDocument(marcxRecord.getBytes()));
        assertMarcExchangeCollection(harvesterRecord.asBytes(), 1);
    }

    @Test
    public void getCharset_returnsUtf8() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        assertThat(StandardCharsets.UTF_8.compareTo(harvesterRecord.getCharset()), is(0));
    }

    @Test
    public void asBytes_collectionContainsNoRecordMembers_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.asBytes();
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void asDocument_collectionContainsNoRecordMembers_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = getMarcExchangeCollection();
        try {
            harvesterRecord.asDocument();
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    void assertMarcExchangeCollection(byte[] data, int expectedMemberCount) {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(new ByteArrayInputStream(data));
            final Element documentElement = document.getDocumentElement();
            assertThat(documentElement, is(notNullValue()));
            assertThat(documentElement.getLocalName(), is(MarcExchangeCollection.COLLECTION_ELEMENT_NAME));
            assertThat(documentElement.getNamespaceURI(), is(MarcExchangeCollection.MARC_EXCHANGE_NAMESPACE));
            assertThat(documentElement.getElementsByTagNameNS(
                    MarcExchangeCollection.MARC_EXCHANGE_NAMESPACE, MarcExchangeCollection.RECORD_ELEMENT_NAME).getLength(),
                    is(expectedMemberCount));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Document asDocument(byte[] data) {
        try {
            return getDocumentBuilder().parse(new ByteArrayInputStream(data));
        } catch (SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private DocumentBuilder getDocumentBuilder() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Transformer getTransformer() {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            return transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private MarcExchangeCollection getMarcExchangeCollection() throws HarvesterException {
        return new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
    }
}