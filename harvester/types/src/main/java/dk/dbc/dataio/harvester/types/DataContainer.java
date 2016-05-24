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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class represents a data container as a harvester XML record.
 * <p>
 * This class is not thread safe.
 * </p>
 */
public class DataContainer implements HarvesterXmlRecord {
    static final String DATA_CONTAINER_ELEMENT_NAME = "data-container";
    static final String DATA_SUPPLEMENTARY_ELEMENT_NAME = "data-supplementary";
    static final String DATA_ELEMENT_NAME = "data";

    private final Transformer transformer;
    private final DocumentBuilder documentBuilder;
    private final Charset charset = StandardCharsets.UTF_8;
    private String creationDate = null;
    private String enrichmentTrail = null;
    private Element data;
    private String trackingId;

    /**
     * Class constructor
     * @param documentBuilder builder used for XML representations
     * @param transformer transformer used for conversion to byte[]
     * @throws NullPointerException if given any null-valued argument
     */
    public DataContainer(DocumentBuilder documentBuilder, Transformer transformer)
            throws NullPointerException {
        this.transformer = InvariantUtil.checkNotNullOrThrow(transformer, "transformer");
        this.documentBuilder = InvariantUtil.checkNotNullOrThrow(documentBuilder, "documentBuilder");
    }

    /**
     * @return this data container XML representation as byte array
     * @throws HarvesterInvalidRecordException if container contains no data
     * @throws HarvesterException if unable to transform internal data container representation
     * to byte array
     */
    @Override
    public byte[] asBytes() throws HarvesterException {
        final Document document = asDocument();
        final Source source = new DOMSource(document.getDocumentElement());
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final Transformer configuredTransformer = configureTransformer();
            configuredTransformer.transform(source, new StreamResult(out));
            return out.toByteArray();
        } catch (IOException | TransformerException e) {
            throw new HarvesterException("Unable to transform data into bytes", e);
        } finally {
            transformer.reset();
        }
    }

    /**
     * @return this data container XML representation as Document
     * @throws HarvesterInvalidRecordException if container contains no data
     */
    @Override
    public Document asDocument() throws HarvesterInvalidRecordException {
        if (data == null) {
            throw new HarvesterInvalidRecordException("Container data can not be null");
        }
        try {
            final Document dataContainer = documentBuilder.newDocument();
            dataContainer.setXmlStandalone(true);
            dataContainer.appendChild(dataContainer.createElement(DATA_CONTAINER_ELEMENT_NAME));
            appendDataSupplementary(dataContainer);
            appendData(dataContainer);
            return dataContainer;
        } finally {
            documentBuilder.reset();
        }
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets supplementary creation date
     * @param creationDate creation date
     * @throws NullPointerException if given null valued creationDate
     */
    public void setCreationDate(Date creationDate) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(creationDate, "creationDate");
        this.creationDate = new SimpleDateFormat("YYYYMMdd").format(creationDate);
    }

    public void setEnrichmentTrail(String enrichmentTrail) {
        this.enrichmentTrail = enrichmentTrail;
    }

    public String getEnrichmentTrail() {
        return enrichmentTrail;
    }

    /**
     * Sets container data
     * @param data data as DOM element
     * @throws NullPointerException if given null-valued data
     */
    public void setData(Element data) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(data, "data");
        this.data = data;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    private void appendDataSupplementary(Document dataContainer) {
        final Element dataSupplementaryElement = dataContainer.createElement(DATA_SUPPLEMENTARY_ELEMENT_NAME);
        if (creationDate != null) {
            final Element creationDateElement = dataContainer.createElement("creationDate");
            creationDateElement.setTextContent(creationDate);
            dataSupplementaryElement.appendChild(creationDateElement);
        }
        if (enrichmentTrail != null) {
            final Element enrichmentTrailElement = dataContainer.createElement("enrichmentTrail");
            enrichmentTrailElement.setTextContent(this.enrichmentTrail);
            dataSupplementaryElement.appendChild(enrichmentTrailElement);
        }
        if(trackingId != null) {
            final Element trackingIdElement = dataContainer.createElement("trackingId");
            trackingIdElement.setTextContent(this.trackingId);
            dataSupplementaryElement.appendChild(trackingIdElement);
        }
        dataContainer.getDocumentElement().appendChild(dataSupplementaryElement);
    }

    private void appendData(Document dataContainer) {
        final Element dataElement = dataContainer.createElement(DATA_ELEMENT_NAME);
        dataElement.appendChild(dataContainer.importNode(data, true));
        dataContainer.getDocumentElement().appendChild(dataElement);
    }

    private Transformer configureTransformer() throws HarvesterException {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        return transformer;
    }
}
