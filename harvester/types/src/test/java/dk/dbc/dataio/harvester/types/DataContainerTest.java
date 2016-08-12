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

import dk.dbc.dataio.commons.types.AddiMetaData.LibraryRules;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

public class DataContainerTest {
    private final HashMap<String, String> namespacePrefixes = new HashMap<>();
    {
        namespacePrefixes.put("marcx", "info:lc/xmlns/marcxchange-v1");
    }

    private final String marcxCollectionSingleRecord =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
              "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
            "</marcx:collection>";

    private final String enrichmentTrail = "trail";
    private final String trackingId = "DBCTrackingId";

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
    public void setTrackingId_trackingIdArgIsNull_returns() throws HarvesterException {
        final DataContainer dataContainer = getDataContainer();
        dataContainer.setTrackingId(null);
    }

    @Test
    public void asDocument_dataContainerHasSupplementaryData_documentRepresentationHasNonEmptySupplementaryData()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        final Date expectedDate = new Date();
        final DataContainer dataContainer = getDataContainer();
        dataContainer.setCreationDate(expectedDate);
        dataContainer.setEnrichmentTrail(enrichmentTrail);
        dataContainer.setTrackingId(trackingId);
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        final Document document = dataContainer.asDocument();
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data/marcx:collection")
                .withNamespaceContext(namespacePrefixes));
        assertThat(document, hasXPath("count(/dataio-harvester-datafile/data-container/data-supplementary/*)",
                equalTo("3")));
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data-supplementary/creationDate/text()",
                equalTo(new SimpleDateFormat("yyyyMMdd").format(expectedDate))));
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data-supplementary/enrichmentTrail/text()",
                equalTo(enrichmentTrail)));
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data-supplementary/trackingId/text()",
                equalTo(trackingId)));
    }

    @Test
    public void asDocument_dataContainerHasLibraryRules_documentRepresentationSupplementaryDataHasLibraryRules()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        final LibraryRules libraryRules = new LibraryRules()
                .withAgencyType("myType")
                .withLibraryRule("rule1", true)
                .withLibraryRule("rule2", false);

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setLibraryRules(libraryRules);
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        final Document document = dataContainer.asDocument();
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data/marcx:collection")
                .withNamespaceContext(namespacePrefixes));
        assertThat(document, hasXPath("count(/dataio-harvester-datafile/data-container/data-supplementary/*)",
                equalTo("2")));
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data-supplementary/agencyType/text()",
                equalTo(libraryRules.agencyType().get())));
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data-supplementary/rules/rule1[text() = 'true']"));
        assertThat(document, hasXPath("/dataio-harvester-datafile/data-container/data-supplementary/rules/rule2[text() = 'false']"));
    }

    @Test
    public void asDocument_dataContainerHasNoSupplementaryData_documentRepresentationHasEmptySupplementaryData()
            throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        final Document document = dataContainer.asDocument();
        assertThat(document, hasXPath("count(/dataio-harvester-datafile/data-container/data-supplementary/*)",
                equalTo("0")));
    }

    @Test
    public void asBytes() throws HarvesterException, IOException, TransformerException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(getDocumentBuilder(), getTransformer());
        marcExchangeCollection.addMember(marcxCollectionSingleRecord.getBytes(StandardCharsets.UTF_8));

        final DataContainer dataContainer = getDataContainer();
        dataContainer.setTrackingId(trackingId);
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        assertThat(dataContainer.asBytes(), is(asBytes(dataContainer.asDocument().getDocumentElement())));
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

    private DataContainer getDataContainer() throws HarvesterException {
        return new DataContainer(getDocumentBuilder(), getTransformer());
    }
}