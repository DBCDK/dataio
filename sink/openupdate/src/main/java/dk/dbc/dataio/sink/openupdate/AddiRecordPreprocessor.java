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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.ExtraRecordData;
import dk.dbc.oss.ns.catalogingupdate.RecordData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;

public class AddiRecordPreprocessor extends DocumentTransformer {
    static final String NAMESPACE_URI             = "dk.dbc.dataio.processing";
    static final String ELEMENT                   = "sink-update-template";
    static final String RECORD_SCHEMA             = "info:lc/xmlns/marcxchange-v1";
    static final String RECORD_PACKAGING          = "xml";
    static final String UPDATE_TEMPLATE_ATTRIBUTE = "updateTemplate";

    private final Document contentDataDocument;
    private final String template;

    public AddiRecordPreprocessor(AddiRecord addiRecord) throws NullPointerException{
        InvariantUtil.checkNotNullOrThrow(addiRecord, "addiRecord");
        try {
            contentDataDocument = byteArrayToDocument(addiRecord.getContentData());
            final Document metaDataDocument = byteArrayToDocument(addiRecord.getMetaData());
            final NodeList nodeList = metaDataDocument.getElementsByTagNameNS(NAMESPACE_URI, ELEMENT);

            if(nodeList.getLength() > 0) { // element found
                template = ((Element) nodeList.item(0)).getAttribute(UPDATE_TEMPLATE_ATTRIBUTE);
            } else {
                throw new IllegalArgumentException(String.format(
                        "No element found matching local name: %s and namespace URI: %s",
                        ELEMENT,
                        NAMESPACE_URI));
            }
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates a bibliographical record
     * @return bibliographical record
     */
    public BibliographicRecord getMarcXChangeRecord() {
        BibliographicRecord bibliographicRecord = new BibliographicRecord();
        bibliographicRecord.setRecordSchema(RECORD_SCHEMA);
        bibliographicRecord.setRecordPacking(RECORD_PACKAGING);

        RecordData recordData = new RecordData();
        recordData.getContent().add(contentDataDocument.getDocumentElement());
        bibliographicRecord.setRecordData(recordData);

        ExtraRecordData extraRecordData = new ExtraRecordData();
        bibliographicRecord.setExtraRecordData(extraRecordData);

        return bibliographicRecord;
    }

    /**
     * Retrieves the template
     * @return template
     */
    public String getTemplate() {
        return template;
    }

}