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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;

public class AddiRecordPreprocessor extends DocumentTransformer {

    static final String ENCODE_AS_2709_ATTRIBUTE  = "encodeAs2709";

    /**
     * This method pre-processes an addi record according to the following rules:
     *
     * If processing tag is found with attribute encodeAs2709 value FALSE, the tag is removed from the meta data.
     *
     * If processing tag is found with attribute encodeAs2709 value TRUE, the tag is removed from the meta data
     * and the content data is converted to iso2709.
     *
     * DBCTrackingId is added as attribute with given value on info element
     *
     * @param addiRecord Addi record to pre-process
     * @param trackingId of chunk item
     * @return the pre-processed Addi record
     * @throws IllegalArgumentException on invalid metadata or content
     */
    public AddiRecord execute(AddiRecord addiRecord, String trackingId) throws IllegalArgumentException {
        try {
            final Document metaDataDocument = byteArrayToDocument(addiRecord.getMetaData());
            final NodeList processingNodeList = metaDataDocument.getElementsByTagNameNS(DATAIO_PROCESSING_NAMESPACE_URI, DATAIO_PROCESSING_ELEMENT);
            byte[] content = addiRecord.getContentData();

            if (processingNodeList.getLength() > 0) { // The processing tag has been located
                final Node processingNode = processingNodeList.item(0);

                if (Boolean.valueOf(((Element) processingNode).getAttribute(ENCODE_AS_2709_ATTRIBUTE))) {
                    final Document contentDataDocument = byteArrayToDocument(addiRecord.getContentData());
                    content = Iso2709Packer.create2709FromMarcXChangeRecord(contentDataDocument, new DanMarc2Charset());
                }
                removeFromDom(processingNodeList);
            }
            final NodeList infoNodeList = metaDataDocument.getElementsByTagNameNS(ES_NAMESPACE_URI, ES_INFO_ELEMENT);
            final Element infoElement = (Element) infoNodeList.item(0);
            infoElement.setAttribute(DBCTrackedLogContext.DBC_TRACKING_ID_KEY, trackingId);

            return new AddiRecord(documentToByteArray(metaDataDocument), content);
        } catch (IOException | SAXException | TransformerException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
