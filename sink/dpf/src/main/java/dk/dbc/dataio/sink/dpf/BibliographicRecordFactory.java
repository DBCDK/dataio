/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData;
import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraDataMarshaller;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.ExtraRecordData;
import dk.dbc.oss.ns.catalogingupdate.RecordData;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class BibliographicRecordFactory extends DocumentTransformer {
    private final BibliographicRecordExtraDataMarshaller bibliographicRecordExtraDataMarshaller =
            new BibliographicRecordExtraDataMarshaller();

    public BibliographicRecord toBibliographicRecord(MarcRecord marcRecord) throws BibliographicRecordFactoryException {
        return toBibliographicRecord(MarcRecordFactory.toMarcXchange(marcRecord));
    }

    public BibliographicRecord toBibliographicRecord(MarcRecord marcRecord, String queueProvider)
            throws BibliographicRecordFactoryException {
        return toBibliographicRecord(MarcRecordFactory.toMarcXchange(marcRecord), queueProvider);
    }

    public BibliographicRecord toBibliographicRecord(byte[] bytes)
            throws BibliographicRecordFactoryException {
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

    public BibliographicRecord toBibliographicRecord(byte[] bytes, String queueProvider)
            throws BibliographicRecordFactoryException {
        final BibliographicRecord bibliographicRecord = toBibliographicRecord(bytes);
        final ExtraRecordData extraRecordData = new ExtraRecordData();
        if (queueProvider != null && !queueProvider.trim().isEmpty()) {
            final BibliographicRecordExtraData bibliographicRecordExtraData = new BibliographicRecordExtraData();
            bibliographicRecordExtraData.setProviderName(queueProvider);
            bibliographicRecordExtraData.setPriority(1000);  // hardcoded default priority

            try {
                extraRecordData.getContent().add(bibliographicRecordExtraDataMarshaller
                        .toXmlDocument(bibliographicRecordExtraData).getDocumentElement());
            } catch (JAXBException e) {
                throw new BibliographicRecordFactoryException(
                        "Unable to add extra data to bibliographic record", e);
            }
        }
        bibliographicRecord.setExtraRecordData(extraRecordData);
        return bibliographicRecord;
    }
}
