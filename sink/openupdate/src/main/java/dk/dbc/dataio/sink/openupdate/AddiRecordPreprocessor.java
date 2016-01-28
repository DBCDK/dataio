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
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.ExtraRecordData;
import dk.dbc.oss.ns.catalogingupdate.RecordData;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

public class AddiRecordPreprocessor extends DocumentTransformer {
    static final String RECORD_SCHEMA    = "info:lc/xmlns/marcxchange-v1";
    static final String RECORD_PACKAGING = "xml";

    public Result preprocess(AddiRecord addiRecord) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(addiRecord, "addiRecord");
        try {
            final Document metaDataDocument = byteArrayToDocument(addiRecord.getMetaData());
            final String trackingId = extractAttributeValue(metaDataDocument, ES_NAMESPACE_URI, ES_INFO_ELEMENT, DBCTrackedLogContext.DBC_TRACKING_ID_KEY);
            final String submitter = extractAttributeValue(metaDataDocument, ES_NAMESPACE_URI, ES_INFO_ELEMENT, "submitter");
            final String template = extractAttributeValue(metaDataDocument, DATAIO_PROCESSING_NAMESPACE_URI, UPDATE_TEMPLATE_ELEMENT, "updateTemplate");
            final BibliographicRecord bibliographicRecord = getMarcXChangeRecord(byteArrayToDocument(addiRecord.getContentData()));
            return new Result(trackingId, submitter, template, bibliographicRecord);
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private BibliographicRecord getMarcXChangeRecord(Document document) {
        BibliographicRecord bibliographicRecord = new BibliographicRecord();
        bibliographicRecord.setRecordSchema(RECORD_SCHEMA);
        bibliographicRecord.setRecordPacking(RECORD_PACKAGING);

        RecordData recordData = new RecordData();
        recordData.getContent().add(document.getDocumentElement());
        bibliographicRecord.setRecordData(recordData);

        ExtraRecordData extraRecordData = new ExtraRecordData();
        bibliographicRecord.setExtraRecordData(extraRecordData);

        return bibliographicRecord;
    }

    public static class Result {
        private final String trackingId;
        private final String submitter;
        private final String template;
        private final BibliographicRecord bibliographicRecord;

        public Result(String trackingId, String submitter, String template, BibliographicRecord bibliographicRecord) {
            this.trackingId = trackingId;
            this.submitter = submitter;
            this.template = template;
            this.bibliographicRecord = bibliographicRecord;
        }
        public String getTrackingId() {
            return trackingId;
        }

        public String getSubmitter() {
            return submitter;
        }

        public String getTemplate() {
            return template;
        }

        public BibliographicRecord getBibliographicRecord() {
            return bibliographicRecord;
        }
    }
}