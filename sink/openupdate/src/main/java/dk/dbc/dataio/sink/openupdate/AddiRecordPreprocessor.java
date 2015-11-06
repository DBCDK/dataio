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
import org.xml.sax.SAXException;

import java.io.IOException;

public class AddiRecordPreprocessor extends DocumentTransformer {
    static final String DATAIO_PROCESSING_NAMESPACE_URI = "dk.dbc.dataio.processing";
    static final String UPDATE_TEMPLATE_ELEMENT         = "sink-update-template";
    static final String ES_NAMESPACE_URI                = "http://oss.dbc.dk/ns/es";
    static final String ES_INFO_ELEMENT                 = "info";
    static final String RECORD_SCHEMA                   = "info:lc/xmlns/marcxchange-v1";
    static final String RECORD_PACKAGING                = "xml";

    private final Document contentDataDocument;
    private final String template;
    private final String submitter;

    public AddiRecordPreprocessor(AddiRecord addiRecord) throws NullPointerException{
        InvariantUtil.checkNotNullOrThrow(addiRecord, "addiRecord");
        try {
            final Document metaDataDocument = byteArrayToDocument(addiRecord.getMetaData());
            submitter = extractAttributeValue(metaDataDocument, ES_NAMESPACE_URI, ES_INFO_ELEMENT, "submitter");
            template = extractAttributeValue(metaDataDocument, DATAIO_PROCESSING_NAMESPACE_URI, UPDATE_TEMPLATE_ELEMENT, "updateTemplate");
            contentDataDocument = byteArrayToDocument(addiRecord.getContentData());
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

   public String getTemplate() {
        return template;
    }

    public String getSubmitter() {
        return submitter;
    }
}