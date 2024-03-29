package dk.dbc.oclc.wciru;

import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.EndpointReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WciruServiceConnector unit tests
 * The test methods of this class uses the following naming convention:
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class WciruServiceConnectorTest {
    private final String baseUrl = "http://test.dbc.dk/oclc-wciru";
    private final String userId = "userId";
    private final String password = "password";
    private final String authenticationToken = userId + "/" + password;
    private final String holdingSymbol = "symbol";
    private final String holdingsAction = "I";
    private final String projectId = "projectId";
    private final String xmlRecord = "<data/>";
    private final String oclcId = "oclcId";

    private final Diagnostic diagnostic = createDiagnostic();
    private final Diagnostic retriableDiagnostic = createRetriableDiagnostic();
    private final Diagnostic suppressedDiagnostic = createSuppressedDiagnotic();

    private final UpdateService updateService = mock(UpdateService.class);
    private MockedUpdateServiceProxy proxy;
    private WciruServiceConnector connector;

    @BeforeEach
    public void init() {
        proxy = new MockedUpdateServiceProxy();
        when(updateService.getUpdate()).thenReturn(proxy);
        connector = new WciruServiceConnector(updateService, baseUrl, userId, password, projectId, newRetryScheme());
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        assertThat(connector, is(notNullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_recordArgIsInvalidXml_throws() {
        assertThrows(WciruServiceConnectorException.class, () -> connector.addOrUpdateRecord("not XML", holdingSymbol, oclcId));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestAction() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.CREATE_ACTION));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestSrwVersion() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestExtraRequestData() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().dataIdentifier, is("Holdings"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getEditReplaceType(), is("I"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getNewValue(), is(holdingSymbol));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestRecord() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_oclcIdArgIsNull_requestRecordIdentifierIsNotSet() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, null);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_oclcIdArgIsEmpty_requestRecordIdentifierIsNotSet() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, "");
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_oclcIdArgIsSet_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_serviceReturnsWithStatusFail_throws() {
        proxy.responses.add(getUpdateResponseWithStatusFail(diagnostic));
        assertThrows(WciruServiceConnectorException.class, () -> connector.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestAction() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.CREATE_ACTION));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestSrwVersion() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestExtraRequestData() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().dataIdentifier, is("Holdings"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getEditReplaceType(), is("I"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getNewValue(), is(holdingSymbol));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestRecord() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_oclcIdArgIsNull_RequestRecordIdentifierIsNotSet() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, null);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_oclcIdArgIsEmpty_RequestRecordIdentifierIsNotSet() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, "");
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_oclcIdArgIsSet_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_serviceReturnsWithStatusFail_throws() {
        proxy.responses.add(getUpdateResponseWithStatusFail(diagnostic));
        assertThrows(WciruServiceConnectorException.class, () -> connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId));
    }

    @Test
    public void replaceRecord_holdingActionArgIsInvalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, "INVALID"));
    }

    @Test
    public void replaceRecord_setsRequestAction() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.REPLACE_ACTION));
    }

    @Test
    public void replaceRecord_setsRequestSrwVersion() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void replaceRecord_setsRequestExtraRequestData() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().dataIdentifier, is("Holdings"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getEditReplaceType(), is(holdingsAction));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getNewValue(), is(holdingSymbol));
    }

    @Test
    public void replaceRecord_holdingActionArgIsD_setsRequestExtraRequestDataOldValue() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, "D");
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getOldValue(), is(holdingSymbol));
    }

    @Test
    public void replaceRecord_setsRequestRecord() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void replaceRecord_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void replaceRecord_serviceReturnsWithStatusFail_throws() {
        proxy.responses.add(getUpdateResponseWithStatusFail(diagnostic));
        assertThrows(WciruServiceConnectorException.class, () -> connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction));
    }

    @Test
    public void deleteRecord_setsRequestAction() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.DELETE_ACTION));
    }

    @Test
    public void deleteRecord_setsRequestSrwVersion() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void deleteRecord_setsRequestExtraRequestData() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
    }

    @Test
    public void deleteRecord_setsRequestRecord() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void deleteRecord_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        connector.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void deleteRecord_serviceReturnsWithStatusFail_throws() {
        proxy.responses.add(getUpdateResponseWithStatusFail(diagnostic));
        assertThrows(WciruServiceConnectorException.class, () -> connector.deleteRecord(getXmlRecordElement(), oclcId));
    }

    @Test
    public void deleteRecordWithRetry_allRetriesAreUsed_throws() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));

        try {
            connector.deleteRecord(getXmlRecordElement(), oclcId);
            fail("An expected exception wasn't thrown!");
        } catch (WciruServiceConnectorRetryException e) {
            assertThat(e.getNumberOfRetries(), is(3));
        }
    }

    @Test
    public void addOrUpdateRecordWithRetry_allRetriesAreUsed_throws() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));

        try {
            connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
            fail("An expected exception wasn't thrown!");
        } catch (WciruServiceConnectorRetryException e) {
            assertThat(e.getNumberOfRetries(), is(3));
        }
    }

    @Test
    public void replaceRecordWithRetry_allRetriesAreUsed_throws() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));

        try {
            connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
            fail("An expected exception wasn't thrown!");
        } catch (WciruServiceConnectorRetryException e) {
            assertThat(e.getNumberOfRetries(), is(3));
        }
    }

    @Test
    public void deleteRecordWithRetry_notAllRetriesAreUsed_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusSuccess());

        connector.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void addOrUpdateRecordWithRetry_notAllRetriesAreUsed_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusSuccess());

        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void replaceRecordWithRetry_notAllRetriesAreUsed_setsRequestRecordIdentifier() throws Exception {
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusFail(retriableDiagnostic));
        proxy.responses.add(getUpdateResponseWithStatusSuccess());

        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void constructorForInnerClassRetryScheme_allArgsAreValid_returnsNewInstance() {
        WciruServiceConnector.RetryScheme instance = new WciruServiceConnector.RetryScheme(0, 0, Collections.emptySet());
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void addOrUpdateRecordReturnsOnSuppressedDiagnostic()
            throws IOException, SAXException, WciruServiceConnectorException {
        proxy.responses.add(getUpdateResponseWithStatusFail(suppressedDiagnostic));
        connector.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
    }

    @Test
    public void deleteRecordReturnsOnSuppressedDiagnostic()
            throws IOException, SAXException, WciruServiceConnectorException {
        proxy.responses.add(getUpdateResponseWithStatusFail(suppressedDiagnostic));
        connector.deleteRecord(getXmlRecordElement(), oclcId);
    }

    @Test
    public void replaceRecordReturnsOnSuppressedDiagnostic()
            throws IOException, SAXException, WciruServiceConnectorException {
        proxy.responses.add(getUpdateResponseWithStatusFail(suppressedDiagnostic));
        connector.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
    }

    private WciruServiceConnector.RetryScheme newRetryScheme() {
        return new WciruServiceConnector.RetryScheme(3, 10,
                new HashSet<>(Collections.singletonList(retriableDiagnostic.getUri())));
    }

    private Element getXmlRecordElement() throws SAXException, IOException {
        return JaxpUtil.parseDocument(xmlRecord).getDocumentElement();
    }

    private static final class MockedUpdateServiceProxy implements UpdateInterface, BindingProvider {
        public UpdateRequestType lastRequest;
        public LinkedList<UpdateResponseType> responses = new LinkedList<>();

        @Override
        public UpdateResponseType update(UpdateRequestType updateRequest) {
            lastRequest = updateRequest;
            return responses.pop();
        }

        @Override
        public ExplainResponseType explain(ExplainRequestType explainRequest) {
            return null;
        }

        @Override
        public Map<String, Object> getRequestContext() {
            return new HashMap<>(1);
        }

        @Override
        public Map<String, Object> getResponseContext() {
            return null;
        }

        @Override
        public Binding getBinding() {
            return null;
        }

        @Override
        public EndpointReference getEndpointReference() {
            return null;
        }

        @Override
        public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
            return null;
        }
    }

    private UpdateResponseType getUpdateResponseWithStatusSuccess() {
        UpdateResponseType response = new UpdateResponseType();
        response.setOperationStatus(OperationStatusType.SUCCESS);
        return response;
    }

    private UpdateResponseType getUpdateResponseWithStatusFail(Diagnostic diagnostic) {
        UpdateResponseType response = new UpdateResponseType();
        response.setOperationStatus(OperationStatusType.FAIL);
        DiagnosticsType diagnostics = new DiagnosticsType();
        diagnostics.getDiagnostic().add(diagnostic);
        response.setDiagnostics(diagnostics);
        return response;
    }

    private Diagnostic createDiagnostic() {
        Diagnostic diagnostic = createRetriableDiagnostic();
        diagnostic.setUri("diagnostic/1/61");
        return diagnostic;
    }

    private Diagnostic createRetriableDiagnostic() {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setUri("diagnostic/1/51");
        diagnostic.setMessage("failure to communicate");
        return diagnostic;
    }

    private Diagnostic createSuppressedDiagnotic() {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setUri("info:srw/diagnostic/12/13");
        diagnostic.setMessage("Invalid data structure: component rejected");
        diagnostic.setDetails("SRU_RemoveLSN_Failures_No_LSN_Found. The PPN [800010-katalog:99122974111405763] was not found in the database record.:Unspecified error(100)");
        return diagnostic;
    }
}
