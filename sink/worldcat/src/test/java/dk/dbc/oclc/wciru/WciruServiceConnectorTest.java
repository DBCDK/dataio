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

package dk.dbc.oclc.wciru;

import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WciruServiceConnector unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class WciruServiceConnectorTest {
    private final String baseUrl = "http://test.dbc.dk/oclc-wciru";
    private final String userId = "userId";
    private final String password = "password";
    private final String authenticationToken = userId+ "/" +password;
    private final String holdingSymbol = "symbol";
    private final String holdingsAction = "I";
    private final String projectId = "projectId";
    private final String xmlRecord = "<data/>";
    private final String oclcId = "oclcId";

    private final UpdateService updateService = mock(UpdateService.class);

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        assertThat(instance, is(notNullValue()));
    }

    @Test(expected=WciruServiceConnectorException.class)
    public void addOrUpdateRecordTakingStringParameter_recordArgIsInvalidXml_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        instance.addOrUpdateRecord("not XML", holdingSymbol, oclcId);
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestAction() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.CREATE_ACTION));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestSrwVersion() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestExtraRequestData() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().dataIdentifier, is("Holdings"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getEditReplaceType(), is("I"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getNewValue(), is(holdingSymbol));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_setsRequestRecord() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_oclcIdArgIsNull_requestRecordIdentifierIsNotSet() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, null);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_oclcIdArgIsEmpty_requestRecordIdentifierIsNotSet() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, "");
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingStringParameter_oclcIdArgIsSet_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test(expected=WciruServiceConnectorException.class)
    public void addOrUpdateRecordTakingStringParameter_serviceReturnsWithStatusFail_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("failure to communicate"));
        instance.addOrUpdateRecord(xmlRecord, holdingSymbol, oclcId);
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestAction() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.CREATE_ACTION));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestSrwVersion() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestExtraRequestData() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().dataIdentifier, is("Holdings"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getEditReplaceType(), is("I"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getNewValue(), is(holdingSymbol));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_setsRequestRecord() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_oclcIdArgIsNull_RequestRecordIdentifierIsNotSet() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, null);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_oclcIdArgIsEmpty_RequestRecordIdentifierIsNotSet() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, "");
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(nullValue()));
    }

    @Test
    public void addOrUpdateRecordTakingElementParameter_oclcIdArgIsSet_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test(expected=WciruServiceConnectorException.class)
    public void addOrUpdateRecordTakingElementParameter_serviceReturnsWithStatusFail_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("failure to communicate"));
        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
    }

    @Test(expected=IllegalArgumentException.class)
    public void replaceRecord_holdingActionArgIsInvalid_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, "INVALID");
    }

    @Test
    public void replaceRecord_setsRequestAction() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.REPLACE_ACTION));
    }

    @Test
    public void replaceRecord_setsRequestSrwVersion() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void replaceRecord_setsRequestExtraRequestData() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().dataIdentifier, is("Holdings"));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getEditReplaceType(), is(holdingsAction));
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getNewValue(), is(holdingSymbol));
    }

    @Test
    public void replaceRecord_holdingActionArgIsD_setsRequestExtraRequestDataOldValue() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, "D");
        assertThat(proxy.lastRequest.extraRequestData.getEditReplace().getOldValue(), is(holdingSymbol));
    }

    @Test
    public void replaceRecord_setsRequestRecord() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void replaceRecord_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test(expected=WciruServiceConnectorException.class)
    public void replaceRecord_serviceReturnsWithStatusFail_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("failure to communicate"));
        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
    }

    @Test
    public void deleteRecord_setsRequestAction() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getAction(), is(WciruServiceConnector.DELETE_ACTION));
    }

    @Test
    public void deleteRecord_setsRequestSrwVersion() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getVersion(), is(WciruServiceConnector.SRW_VERSION));
    }

    @Test
    public void deleteRecord_setsRequestExtraRequestData() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.extraRequestData.getAuthenticationToken(), is(authenticationToken));
        assertThat(proxy.lastRequest.extraRequestData.getProjectid(), is(projectId));
    }

    @Test
    public void deleteRecord_setsRequestRecord() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getRecord().getRecordPacking(), is(WciruServiceConnector.RECORD_PACKING));
        assertThat(proxy.lastRequest.getRecord().getRecordSchema(), is(WciruServiceConnector.RECORD_SCHEMA));
        Element expectedRecordData = getXmlRecordElement();
        Element actualRecordData = (Element) proxy.lastRequest.getRecord().getRecordData().getContent().get(0);
        assertThat(actualRecordData.toString(), is(expectedRecordData.toString()));
    }

    @Test
    public void deleteRecord_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusSuccess());
        instance.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test(expected=WciruServiceConnectorException.class)
    public void deleteRecord_serviceReturnsWithStatusFail_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("failure to communicate"));
        instance.deleteRecord(getXmlRecordElement(), oclcId);
    }

    @Test
    public void deleteRecordWithRetry_allRetriesAreUsed_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));

        try {
            instance.deleteRecord(getXmlRecordElement(), oclcId);
            fail("An expected exception wasn't thrown!");
        } catch (WciruServiceConnectorRetryException e) {
            assertThat(e.getNumberOfRetries(), is(3));
        }
    }

    @Test
    public void addOrUpdateRecordWithRetry_allRetriesAreUsed_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));

        try {
            instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
            fail("An expected exception wasn't thrown!");
        } catch (WciruServiceConnectorRetryException e) {
            assertThat(e.getNumberOfRetries(), is(3));
        }
    }

    @Test
    public void replaceRecordWithRetry_allRetriesAreUsed_throws() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));

        try {
            instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
            fail("An expected exception wasn't thrown!");
        } catch (WciruServiceConnectorRetryException e) {
            assertThat(e.getNumberOfRetries(), is(3));
        }
    }

    @Test
    public void deleteRecordWithRetry_notAllRetriesAreUsed_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusSuccess());

        instance.deleteRecord(getXmlRecordElement(), oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void addOrUpdateRecordWithRetry_notAllRetriesAreUsed_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusSuccess());

        instance.addOrUpdateRecord(getXmlRecordElement(), holdingSymbol, oclcId);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void replaceRecordWithRetry_notAllRetriesAreUsed_setsRequestRecordIdentifier() throws Exception {
        WciruServiceConnector instance = getDefaultWciruServiceConnector();
        MockedUpdateServiceProxy proxy = getMockedUpdateServiceProxy();
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusFail("diagnostic/1/51", "failure to communicate"));
        proxy.responses.add(getUpdateResponseWithStatusSuccess());

        instance.replaceRecord(getXmlRecordElement(), oclcId, holdingSymbol, holdingsAction);
        assertThat(proxy.lastRequest.getRecordIdentifier(), is(oclcId));
    }

    @Test
    public void constructorForInnerClassRetryScheme_allArgsAreValid_returnsNewInstance() {
        Set<String> emptySet = Collections.emptySet();
        WciruServiceConnector.RetryScheme instance = new WciruServiceConnector.RetryScheme(0, 0, emptySet);
        assertThat(instance, is(notNullValue()));
    }

    //--------------------------------------------------------------------------

    private WciruServiceConnector getDefaultWciruServiceConnector() {
        return new WciruServiceConnector(baseUrl, userId, password, projectId, getDefaultTestRetryScheme(), updateService);
    }

    private WciruServiceConnector.RetryScheme getDefaultTestRetryScheme() {
        return new WciruServiceConnector.RetryScheme(3, 10, new HashSet<>(Arrays.asList("diagnostic/1/51")));
    }

    private MockedUpdateServiceProxy getMockedUpdateServiceProxy() {
        final MockedUpdateServiceProxy proxy = new MockedUpdateServiceProxy();
        when(updateService.getUpdate()).thenReturn(proxy);
        return proxy;
    }

    private Element getXmlRecordElement() throws ParserConfigurationException, SAXException, IOException {
        return JaxpUtil.parseDocument(xmlRecord).getDocumentElement();
    }

    private final class MockedUpdateServiceProxy implements UpdateInterface, BindingProvider {
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

    private UpdateResponseType getUpdateResponseWithStatusFail(String diagnosticMessage) {
        return getUpdateResponseWithStatusFail("", diagnosticMessage);
    }
    
    private UpdateResponseType getUpdateResponseWithStatusFail(String uri, String diagnosticMessage) {
        UpdateResponseType response = new UpdateResponseType();
        response.setOperationStatus(OperationStatusType.FAIL);
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setUri(uri);
        diagnostic.setMessage(diagnosticMessage);
        DiagnosticsType diagnostics = new DiagnosticsType();
        diagnostics.getDiagnostic().add(diagnostic);
        response.setDiagnostics(diagnostics);
        return response;
    }
}
