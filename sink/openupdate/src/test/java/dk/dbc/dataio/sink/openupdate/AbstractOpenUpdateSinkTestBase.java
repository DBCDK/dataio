package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResponse;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xmlunit.matchers.CompareMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class AbstractOpenUpdateSinkTestBase {
    public static final String UPDATE_RECORD_RESULT_WITH_VALIDATION_ERROR = "/updateRecordResult.validationError.xml";
    public static final String UPDATE_RECORD_RESULT_OK = "/updateRecordResult.ok.xml";
    public static final String MARC_EXCHANGE_WEBSERVICE_FAIL = "/870970.fail.xml";
    public static final String MARC_EXCHANGE_WEBSERVICE_OK = "/870970.ok.xml";

    public static final String DBC_TRACKING_ID = "wiremock-test";

    protected static CompareMatcher isEquivalentTo(Object control) {
        return CompareMatcher.isSimilarTo(control)
                .throwComparisonFailure()
                .normalizeWhitespace()
                .ignoreComments();
    }

    protected static UpdateRecordResponse unmarshalUpdateRecordResponse(byte[] xmlResponseToUnmarshal) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(UpdateRecordResponse.class).createUnmarshaller();
        StringReader reader = new StringReader(StringUtil.asString(xmlResponseToUnmarshal));
        return (UpdateRecordResponse) unmarshaller.unmarshal(reader);
    }

    protected String getMetaXml(String template, String submitter) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" DBCTrackingId=\"" + DBC_TRACKING_ID + "\" submitter=\"" + submitter + "\"/>" +
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" updateTemplate=\"" + template + "\"/>" +
                "</es:referencedata>";
    }

    protected String getInvalidMetaXml(String referenceDataChildren) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                referenceDataChildren +
                "</es:referencedata>";
    }

    protected String getContentXml() {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">field1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    protected AddiRecord newAddiRecord(String meta, String content) {
        return new AddiRecord(
                meta.trim().getBytes(StandardCharsets.UTF_8),
                content.trim().getBytes(StandardCharsets.UTF_8));
    }

    protected byte[] addiToBytes(AddiRecord... addiRecords) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (AddiRecord addiRecord : addiRecords) {
                baos.write(addiRecord.getBytes());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    protected UpdateRecordResult getWebserviceResultValidatedOk() throws JAXBException {
        return unmarshalUpdateRecordResponse(readTestRecord(UPDATE_RECORD_RESULT_OK)).getUpdateRecordResult();
    }

    protected UpdateRecordResult getWebserviceResultWithValidationErrors() throws JAXBException {
        return unmarshalUpdateRecordResponse(readTestRecord(UPDATE_RECORD_RESULT_WITH_VALIDATION_ERROR)).getUpdateRecordResult();
    }

    protected byte[] readTestRecord(String resourceName) {
        try {
            URL url = AbstractOpenUpdateSinkTestBase.class.getResource(resourceName);
            return Files.readAllBytes(Paths.get(url.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
