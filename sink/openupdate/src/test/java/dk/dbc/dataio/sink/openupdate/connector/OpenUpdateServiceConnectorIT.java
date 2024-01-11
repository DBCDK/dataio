package dk.dbc.dataio.sink.openupdate.connector;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.openupdate.AbstractOpenUpdateSinkTestBase;
import dk.dbc.dataio.sink.openupdate.AddiRecordPreprocessor;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@WireMockTest
public class OpenUpdateServiceConnectorIT extends AbstractOpenUpdateSinkTestBase {

    private final String groupId = "010100";
    private final String updateTemplate = "dbc";
    private final String queueProvider = "queue";
    public OpenUpdateServiceConnector openUpdateServiceConnector;

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        openUpdateServiceConnector = new OpenUpdateServiceConnector(wireMockRuntimeInfo.getHttpBaseUrl() + "/UpdateService/2.0");
    }

    @Test
    public void updateRecord_ok() {
        UpdateRecordResult updateRecordResult = getUpdateRecordOkResult();

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, is(notNullValue()));
        assertThat("status", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.OK));
        assertThat("no messages", updateRecordResult.getMessages(), is(nullValue()));
    }

    @Test
    public void updateRecord_fail() {
        UpdateRecordResult updateRecordResult = getUpdateRecordFailedResult();

        // Verification
        assertThat("UpdateRecordResult", updateRecordResult, is(notNullValue()));
        assertThat("status", updateRecordResult.getUpdateStatus(), is(UpdateStatusEnum.FAILED));
        assertThat("messages", updateRecordResult.getMessages().getMessageEntry().size(), is(4));
    }

    void recordUpdateRecordRequests() {
        getUpdateRecordOkResult();
        getUpdateRecordFailedResult();
    }

    private UpdateRecordResult getUpdateRecordOkResult() {
        AddiRecord addiRecord = new AddiRecord(
                getMetaXml(updateTemplate, groupId).getBytes(StandardCharsets.UTF_8),
                readTestRecord(MARC_EXCHANGE_WEBSERVICE_OK));
        BibliographicRecord bibliographicRecord = getBibliographicRecord(queueProvider, addiRecord);
        return openUpdateServiceConnector.updateRecord(groupId, updateTemplate, bibliographicRecord, DBC_TRACKING_ID);
    }

    private UpdateRecordResult getUpdateRecordFailedResult() {
        AddiRecord addiRecord = new AddiRecord(
                getMetaXml(updateTemplate, groupId).getBytes(StandardCharsets.UTF_8),
                readTestRecord(MARC_EXCHANGE_WEBSERVICE_FAIL));
        BibliographicRecord bibliographicRecord = getBibliographicRecord(queueProvider, addiRecord);
        return openUpdateServiceConnector.updateRecord(groupId, updateTemplate, bibliographicRecord, DBC_TRACKING_ID);
    }

    private BibliographicRecord getBibliographicRecord(String queueProvider, AddiRecord addiRecord) {
        AddiRecordPreprocessor.Result preprocessorResult = new AddiRecordPreprocessor().preprocess(addiRecord, queueProvider);
        return preprocessorResult.getBibliographicRecord();
    }
}
