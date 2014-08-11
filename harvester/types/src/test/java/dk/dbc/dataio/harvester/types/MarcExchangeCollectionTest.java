package dk.dbc.dataio.harvester.types;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    @Test
    public void addMember_memberDataArgIsNull_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(null);
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIsNotValidXml_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember("invalid-xml".getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgDoesNotIndicateUtf8Encoding_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxRecordNonUtf8Encoding.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIncorrectNamespace_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxCollectionInvalidNamespace.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIncorrectChildNamespace_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxCollectionInvalidMemberNamespace.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIsCollectionWithMultipleRecords_throws() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(marcxCollectionMultipleRecords.getBytes());
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void addMember_memberDataArgIsMarcxCollectionWithSingleRecord_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        harvesterRecord.addMember(marcxCollectionSingleRecord.getBytes());
        assertMarcExchangeCollection(harvesterRecord.getData(), 1);
    }

    @Test
    public void addMember_memberDataArgIsMarcxRecord_recordIsAddedToCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        harvesterRecord.addMember(marcxRecord.getBytes());
        assertMarcExchangeCollection(harvesterRecord.getData(), 1);
    }

    @Test
    public void addMember_calledMultipleTimes_multipleRecordsInCollection() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        harvesterRecord.addMember(marcxCollectionSingleRecord.getBytes());
        harvesterRecord.addMember(marcxRecord.getBytes());
        assertMarcExchangeCollection(harvesterRecord.getData(), 2);
    }

    @Test
    public void getCharset_returnsUtf8() throws HarvesterException {
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        assertThat(StandardCharsets.UTF_8.compareTo(harvesterRecord.getCharset()), is(0));
    }

    private void assertMarcExchangeCollection(byte[] data, int expectedMemberCount) {
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
}