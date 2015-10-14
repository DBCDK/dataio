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
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;

public class AddiRecordPreprocessor {
    static final String PROCESSING_TAG  = "dataio:sink-processing";
    static final String NODE_NAME       = "encodeAs2709";

    /**
     * This method pre-processes an addi record according to the following rules:
     *
     * If the processing tag(dataio:sink-processing) is not found within the meta data,
     * the Addi record is returned unchanged
     *
     * If processing tag is found with value FALSE, the tag is removed from the meta data.
     *
     * If processing tag is found with value TRUE, the tag is removed from the meta data
     * and the content data is converted to iso2709.
     *
     * @param addiRecord Addi record to pre-process
     * @return the pre-processed Addi record
     * @throws IllegalArgumentException on invalid metadata or content
     */
    public AddiRecord execute(AddiRecord addiRecord) throws IllegalArgumentException {
        try {
            final Document metaDataDocument = DocumentTransformer.byteArrayToDocument(addiRecord.getMetaData());
            final NodeList nodeList = metaDataDocument.getElementsByTagName(PROCESSING_TAG);

            if (nodeList.getLength() > 0) { // The specific tag has been located
                byte[] content = addiRecord.getContentData();
                final Node processingNode = nodeList.item(0);

                if (Boolean.valueOf(DocumentTransformer.getNodeValue(NODE_NAME, processingNode))) {
                    final Document contentDataDocument = DocumentTransformer.byteArrayToDocument(addiRecord.getContentData());
                    content = Iso2709Packer.create2709FromMarcXChangeRecord(contentDataDocument, new DanMarc2Charset());
                }

                DocumentTransformer.removeFromDom(nodeList);
                return new AddiRecord(DocumentTransformer.documentToByteArray(metaDataDocument), content);
            }
            return addiRecord;
        } catch (IOException | SAXException | TransformerException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
