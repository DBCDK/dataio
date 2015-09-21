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

package dk.dbc.dataio.harvester.types;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DataContainerTest {
    private final String marcxCollectionSingleRecord =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
              "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
            "</marcx:collection>";

    @Test(expected = NullPointerException.class)
    public void constructor_documentBuilderArgIsNull_throws() {
        new DataContainer(null, getTransformer());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_transformerArgIsNull_throws() {
        new DataContainer(getDocumentBuilder(), null);
    }

    @Test
    public void asBytes_containerContainsNoData_throws() throws HarvesterException {
        final DataContainer dataContainer = getDataContainer();
        try {
            dataContainer.asBytes();
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void asDocument_containerContainsNoData_throws() throws HarvesterException {
        final DataContainer dataContainer = getDataContainer();
        try {
            dataContainer.asDocument();
            fail("No exception thrown");
        } catch (HarvesterInvalidRecordException e) {
        }
    }

    @Test
    public void setData_dataArgIsNull_throws() throws HarvesterException {
        final DataContainer dataContainer = getDataContainer();
        try {
            dataContainer.setData(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void setCreationDate_creationDateArgIsNull_throws() throws HarvesterException {
        final DataContainer dataContainer = getDataContainer();
        try {
            dataContainer.setCreationDate(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void setEnrichmentTrail_enrichmentTrailArgIsNull_returns() throws HarvesterException {
        final DataContainer dataContainer = getDataContainer();
        dataContainer.setEnrichmentTrail(null);
    }

    @Test
    public void asDocument_dataContainerHasSupplementaryData_documentRepresentationHasNonEmptySupplementaryData()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        Map<String, String> expectedSupplementaryData = new HashMap<>();
        final Date expectedDate = new Date();
        final String expectedEnrichmentTrail = "trail";
        expectedSupplementaryData.put("creationDate", new SimpleDateFormat("YYYYMMdd").format(expectedDate));
        expectedSupplementaryData.put("enrichmentTrail", expectedEnrichmentTrail);

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setCreationDate(expectedDate);
        dataContainer.setEnrichmentTrail(expectedEnrichmentTrail);
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        assertDataContainerDocument(dataContainer.asDocument(), expectedSupplementaryData);
    }

    @Test
    public void asDocument_dataContainerHasNoSupplementaryData_documentRepresentationHasEmptySupplementaryData()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        assertDataContainerDocument(dataContainer.asDocument(), new HashMap<String, String>(0));
    }

    @Test
    public void asBytes_dataContainerHasSupplementaryData_bytesRepresentationHasNonEmptySupplementaryData()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        Map<String, String> expectedSupplementaryData = new HashMap<>();
        final Date expectedDate = new Date();
        final String expectedEnrichmentTrail = "trail";
        expectedSupplementaryData.put("creationDate", new SimpleDateFormat("YYYYMMdd").format(expectedDate));
        expectedSupplementaryData.put("enrichmentTrail", expectedEnrichmentTrail);

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setCreationDate(expectedDate);
        dataContainer.setEnrichmentTrail(expectedEnrichmentTrail);
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        assertDataContainerDocument(asDocument(dataContainer.asBytes()), expectedSupplementaryData);
    }

    @Test
    public void asBytes_dataContainerHasNoSupplementaryData_bytesRepresentationHasEmptySupplementaryData()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        assertDataContainerDocument(asDocument(dataContainer.asBytes()), new HashMap<String, String>(0));
    }

    private void assertDataContainerDocument(Document dataContainerDocument, Map<String, String> expectedSupplementaryData)
            throws HarvesterInvalidRecordException, IOException, TransformerException {
        final Element documentElement = dataContainerDocument.getDocumentElement();
        assertThat(documentElement, is(notNullValue()));
        final NodeList childNodes = documentElement.getChildNodes();
        assertThat(childNodes.getLength(), is(2));

        assertDataContainerSupplementaryData(childNodes.item(0), expectedSupplementaryData);
        assertDataContainerData(childNodes.item(1));
    }

    private void assertDataContainerData(Node dataNode) throws IOException, TransformerException {
        final NodeList childNodes = dataNode.getChildNodes();
        assertThat(childNodes.getLength(), is(1));
        final MarcExchangeCollectionTest marcExchangeCollectionTest = new MarcExchangeCollectionTest();
        marcExchangeCollectionTest.assertMarcExchangeCollection(asBytes(childNodes.item(0)), 1);
    }

    private void assertDataContainerSupplementaryData(Node supplementaryDataNode, Map<String, String> expectedSupplementaryData) {
        final NodeList childNodes = supplementaryDataNode.getChildNodes();
        assertThat("Number of supplementary data nodes", childNodes.getLength(), is(expectedSupplementaryData.size()));
        for (Map.Entry<String, String> entry : expectedSupplementaryData.entrySet()) {
            final NodeList elementsByTagName = ((Element) supplementaryDataNode).getElementsByTagName(entry.getKey());
            assertThat(entry.getKey() + " elements", elementsByTagName.getLength(), is(1));
            assertThat(entry.getKey() + " value",  elementsByTagName.item(0).getTextContent(), is(entry.getValue()));
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

    private byte[] asBytes(Node node) throws IOException, TransformerException {
        final Transformer transformer = getTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        final Source source = new DOMSource(node);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            transformer.transform(source, new StreamResult(out));
            return out.toByteArray();
        }
    }

    private Document asDocument(byte[] data) {
        try {
            return getDocumentBuilder().parse(new ByteArrayInputStream(data));
        } catch (SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private DataContainer getDataContainer() throws HarvesterException {
        return new DataContainer(getDocumentBuilder(), getTransformer());
    }
}