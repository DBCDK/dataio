/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.RecordData;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

class BibliographicRecordFactory extends DocumentTransformer {
    BibliographicRecord toBibliographicRecord(MarcRecord marcRecord) throws BibliographicRecordFactoryException {
        return toBibliographicRecord(MarcRecordFactory.toMarcXchange(marcRecord));
    }

    BibliographicRecord toBibliographicRecord(byte[] bytes) throws BibliographicRecordFactoryException {
        try {
            final Document document = byteArrayToDocument(bytes);
            final BibliographicRecord bibliographicRecord = new BibliographicRecord();
            bibliographicRecord.setRecordSchema("info:lc/xmlns/marcxchange-v1");
            bibliographicRecord.setRecordPacking("xml");

            final RecordData recordData = new RecordData();
            recordData.getContent().add(document.getDocumentElement());

            bibliographicRecord.setRecordData(recordData);
            return bibliographicRecord;
        } catch (IOException | SAXException e) {
            throw new BibliographicRecordFactoryException(
                    "Unable to create double record check bibliographic record", e);
        }
    }
}
