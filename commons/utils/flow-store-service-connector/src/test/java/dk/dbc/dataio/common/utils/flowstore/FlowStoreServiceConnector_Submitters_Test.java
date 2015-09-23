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

package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.ID;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.NUMBER;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.VERSION;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,})
public class FlowStoreServiceConnector_Submitters_Test {

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // ************************************** create submitter tests **************************************
    @Test(expected = NullPointerException.class)
    public void createSubmitter_submitterContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createSubmitter(null);
    }

    @Test
    public void createSubmitter_submitterIsCreated_returnsSubmitter() throws FlowStoreServiceConnectorException, JSONBException {
        final Submitter expectedSubmitter = new SubmitterBuilder().build();
        final Submitter submitter = createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedSubmitter);
        assertThat(submitter, is(notNullValue()));
        assertThat(submitter.getId(), is(expectedSubmitter.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createSubmitter_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createSubmitter_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "");
    }

    // Helper method
    private Submitter createSubmitter_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        when(HttpClient.doPostWithJson(CLIENT, submitterContent, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createSubmitter(submitterContent);
    }

    // **************************************** get submitter tests ****************************************
    @Test
    public void getSubmitter_submitterRetrieved_returnsSubmitter() throws FlowStoreServiceConnectorException {
        final Submitter expectedSubmitterResult = new SubmitterBuilder().build();
        final Submitter submitterResult = getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedSubmitterResult);
        assertThat(submitterResult, is(notNullValue()));
        assertThat(submitterResult.getId(), is(expectedSubmitterResult.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSubmitter_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getSubmitter_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    private Submitter getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER)
                .bind(FlowStoreServiceConstants.SUBMITTER_ID_VARIABLE, ID);
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getSubmitter(ID);
    }

    // ************************************ get submitter by submitter number tests ************************************
    @Test
    public void getSubmitterBySubmitterNumber_submitterRetrieved_returnsSubmitter() throws FlowStoreServiceConnectorException {
        final Submitter expectedSubmitterResult = new SubmitterBuilder().setContent(new SubmitterContentBuilder().setNumber(NUMBER).build()).build();
        final Submitter submitterResult = getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedSubmitterResult);
        assertThat(submitterResult, is(notNullValue()));
        assertThat(submitterResult.getContent().getNumber(), is(expectedSubmitterResult.getContent().getNumber()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSubmitterBySubmitterNumber_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getSubmitterBySubmitterNumber_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        getSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getSubmitterByNumber_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        getSubmitterBySubmitterNumber_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    private Submitter getSubmitterBySubmitterNumber_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_SEARCHES_NUMBER)
                .bind(FlowStoreServiceConstants.SUBMITTER_NUMBER_VARIABLE, NUMBER);
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getSubmitterBySubmitterNumber(NUMBER);
    }

    // **************************************** update submitter tests ****************************************
    @Test
    public void updateSubmitter_submitterIsUpdated_returnsSubmitter() throws FlowStoreServiceConnectorException, JSONBException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        final Submitter submitterToUpdate = new SubmitterBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_CONTENT)
                .bind(FlowStoreServiceConstants.SUBMITTER_ID_VARIABLE, submitterToUpdate.getId());
        when(HttpClient.doPostWithJson(CLIENT, headers, submitterContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), submitterToUpdate));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        Submitter updatedSubmitter = instance.updateSubmitter(submitterContent, submitterToUpdate.getId(), submitterToUpdate.getVersion());

        assertThat(updatedSubmitter, is(notNullValue()));
        assertThat(updatedSubmitter.getContent(), is(notNullValue()));
        assertThat(updatedSubmitter.getId(), is(submitterToUpdate.getId()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void updateSubmitter_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSubmitter_responseWithPrimaryKeyViolation_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSubmitter_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void updateSubmitter_responseWithSubmitterIDNotFound_throws() throws FlowStoreServiceConnectorException {
        updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    // Helper method
    private void updateSubmitter_mockedHttpWithSpecifiedReturnErrorCode(int statusCode) throws FlowStoreServiceConnectorException {
        final SubmitterContent submitterContent = new SubmitterContentBuilder().build();
        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_CONTENT)
                .bind(FlowStoreServiceConstants.SUBMITTER_ID_VARIABLE, ID);
        when(HttpClient.doPostWithJson(CLIENT, headers, submitterContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, ""));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.updateSubmitter(submitterContent, ID, VERSION);
    }

    // *********************************** find all submitters tests ***********************************
    @Test
    public void findAllSubmitters_submittersRetrieved_returnsSubmitters() throws FlowStoreServiceConnectorException {

        final SubmitterContent submitterContentA = new SubmitterContentBuilder().setName("a").setNumber(1L).setDescription("submitterA").build();
        final SubmitterContent submitterContentB = new SubmitterContentBuilder().setName("B").setNumber(2L).setDescription("submitterB").build();
        final Submitter expectedSubmitterResultA = new SubmitterBuilder().setContent(submitterContentA).build();
        final Submitter expectedSubmitterResultB = new SubmitterBuilder().setContent(submitterContentB).build();

        List<Submitter> expectedSubmitterResultList = new ArrayList<>();
        expectedSubmitterResultList.add(expectedSubmitterResultA);
        expectedSubmitterResultList.add(expectedSubmitterResultB);

        final List<Submitter> submitterResultList =
                findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expectedSubmitterResultList);

        assertNotNull(submitterResultList);
        assertFalse(submitterResultList.isEmpty());
        assertThat(submitterResultList.size(), is(2));

        for (Submitter submitter : submitterResultList) {
            assertThat(submitter, is(notNullValue()));
        }
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSubmitters_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test
    public void findAllSubmitters_noResults() throws FlowStoreServiceConnectorException {
        List<Submitter> submitterResultList =
                findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new ArrayList<Submitter>());
        assertThat(submitterResultList, is(notNullValue()));
        assertThat(submitterResultList.size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void findAllSubmitters_noListReturned() throws FlowStoreServiceConnectorException {
        findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void findAllSubmitters_responseWithNotFound_throws() throws FlowStoreServiceConnectorException {
        findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    // Helper method
    private List<Submitter> findAllSubmitters_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, FlowStoreServiceConstants.SUBMITTERS))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.findAllSubmitters();
    }
}
