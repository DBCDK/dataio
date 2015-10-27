package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import org.xmlunit.matchers.CompareMatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class AbstractOpenUpdateSinkTestBase {
    protected static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }

    protected String getMetaXml(String attribute) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"" + " " + attribute + "=\"bog\" charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    protected String getMetaXmlWithoutUpdateElement() {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                " charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    protected String getContentXml() {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">field1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    protected AddiRecord toAddiRecord(byte[] data) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return addiReader.getNextRecord();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected byte[] getAddi(String metaXml, String contentXml) {
        return new AddiRecordWrapper(metaXml, contentXml).getAddiRecordAsString().getBytes();
    }
    protected byte[] getAddi(List<AddiRecordWrapper> addiRecords) {

        return addiRecords.stream().map(AddiRecordWrapper::getAddiRecordAsString).collect(Collectors.joining(System.lineSeparator())).getBytes();
    }

    protected class AddiRecordWrapper {
        private String metaXml;
        private String contentXml;

        public AddiRecordWrapper(String metaXml, String contentXml) {
            this.metaXml = metaXml;
            this.contentXml = contentXml;
        }

        public String getMetaXml() {return this.metaXml;}
        public String getContentXml() {return this.contentXml;}

        private byte[] getMetaXmlAsBytes() {return this.metaXml.trim().getBytes();}
        private byte[] getContentXmlAsBytes() {return this.contentXml.trim().getBytes();}

        public String getAddiRecordAsString() {
            return this.getMetaXmlAsBytes().length
                    + System.lineSeparator()
                    + this.getMetaXml()
                    + System.lineSeparator()
                    + this.getContentXmlAsBytes().length
                    + System.lineSeparator()
                    + this.getContentXml();
        }
    }
}
