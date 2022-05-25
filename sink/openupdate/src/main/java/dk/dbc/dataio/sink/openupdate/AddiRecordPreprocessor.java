package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData;
import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraDataMarshaller;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.ExtraRecordData;
import dk.dbc.oss.ns.catalogingupdate.RecordData;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class AddiRecordPreprocessor extends DocumentTransformer {
    static final String RECORD_SCHEMA    = "info:lc/xmlns/marcxchange-v1";
    static final String RECORD_PACKAGING = "xml";

    private final BibliographicRecordExtraDataMarshaller bibliographicRecordExtraDataMarshaller =
            new BibliographicRecordExtraDataMarshaller();

    public Result preprocess(AddiRecord addiRecord, String queueProvider) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(addiRecord, "addiRecord");
        try {
            final Document metaDataDocument = byteArrayToDocument(addiRecord.getMetaData());
            final String submitter = extractAttributeValue(metaDataDocument, ES_NAMESPACE_URI, ES_INFO_ELEMENT, "submitter");
            final String template = extractAttributeValue(metaDataDocument, DATAIO_PROCESSING_NAMESPACE_URI, UPDATE_TEMPLATE_ELEMENT, "updateTemplate");
            final BibliographicRecord bibliographicRecord = getBibliographicRecord(byteArrayToDocument(addiRecord.getContentData()));
            setExtraRecordData(bibliographicRecord, queueProvider);
            return new Result(submitter, template, bibliographicRecord);
        } catch (IOException | SAXException | JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private BibliographicRecord getBibliographicRecord(Document document) {
        final BibliographicRecord bibliographicRecord = new BibliographicRecord();
        bibliographicRecord.setRecordSchema(RECORD_SCHEMA);
        bibliographicRecord.setRecordPacking(RECORD_PACKAGING);

        final RecordData recordData = new RecordData();
        recordData.getContent().add(document.getDocumentElement());

        bibliographicRecord.setRecordData(recordData);
        return bibliographicRecord;
    }

    private void setExtraRecordData(BibliographicRecord bibliographicRecord, String queueProvider) throws JAXBException {
        final ExtraRecordData extraRecordData = new ExtraRecordData();
        if (queueProvider != null) {
            final BibliographicRecordExtraData bibliographicRecordExtraData = new BibliographicRecordExtraData();
            bibliographicRecordExtraData.setProviderName(queueProvider);
            bibliographicRecordExtraData.setPriority(1000);  // hardcoded default priority

            extraRecordData.getContent().add(bibliographicRecordExtraDataMarshaller
                    .toXmlDocument(bibliographicRecordExtraData).getDocumentElement());
        }
        bibliographicRecord.setExtraRecordData(extraRecordData);
    }

    public static class Result {
        private final String submitter;
        private final String template;
        private final BibliographicRecord bibliographicRecord;

        public Result(String submitter, String template, BibliographicRecord bibliographicRecord) {
            this.submitter = submitter;
            this.template = template;
            this.bibliographicRecord = bibliographicRecord;
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
