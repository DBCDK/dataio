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

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.FailSafeHttpClient;
import dk.dbc.dataio.commons.utils.httpclient.HttpDelete;
import dk.dbc.dataio.commons.utils.httpclient.HttpGet;
import dk.dbc.dataio.commons.utils.httpclient.HttpPost;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowStoreServiceConnector_Sinks_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // **************************************** create sink tests ****************************************
    @Test
    public void createSink_sinkIsCreated_returnsSink() throws FlowStoreServiceConnectorException, JSONBException {
        final Sink expectedSink = new SinkBuilder().build();
        final Sink sink = createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedSink);
        assertThat(sink, is(expectedSink));
    }

    @Test
    public void createSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Sink createSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();

        final HttpPost httpPost = new HttpPost(failSafeHttpClient.getHttpClient())
                    .withBaseUrl(FLOW_STORE_URL)
                    .withPathElements(FlowStoreServiceConstants.SINKS)
                    .withJsonData(sinkContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.createSink(sinkContent);
    }

    // **************************************** get sink tests ****************************************
    @Test
    public void getSink_sinkRetrieved_returnsSink() throws FlowStoreServiceConnectorException {
        final Sink expected = new SinkBuilder().build();
        final Sink sink = getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected, expected.getId());
        assertThat(sink, is(expected));
    }

    @Test
    public void getSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getSink_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null, 1),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private Sink getSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SINK)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient.getHttpClient())
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getSink(id);
    }

    // **************************************** update sink tests ****************************************
    @Test
    public void updateSink_sinkIsUpdated_returnsSink() throws FlowStoreServiceConnectorException, JSONBException {
        final Sink sinkToUpdate = new SinkBuilder().build();
        final Sink updatedSink = updateSink_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), sinkToUpdate, sinkToUpdate.getId(), sinkToUpdate.getVersion());
        assertThat(updatedSink, is(sinkToUpdate));
    }

    @Test
    public void updateSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "", 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private Sink updateSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue, long id, long version) throws FlowStoreServiceConnectorException {
        final SinkContent sinkContent = new SinkContentBuilder().build();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SINK_CONTENT)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        final HttpPost httpPost = new HttpPost(failSafeHttpClient.getHttpClient())
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                .withJsonData(sinkContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateSink(sinkContent, id, version);
    }

    // **************************************** delete sink tests ****************************************
    @Test
    public void deleteSink_sinkIsDeleted() throws FlowStoreServiceConnectorException {
        deleteSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NO_CONTENT.getStatusCode(), 1, 1);
    }

    @Test
    public void deleteSink_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> deleteSink_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), 1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private void deleteSink_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, long id, long version) throws FlowStoreServiceConnectorException {
        final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.SINK)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient.getHttpClient())
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(pathBuilder.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version));

        when(failSafeHttpClient.execute(httpDelete))
                .thenReturn(new MockedResponse<>(statusCode, null));

        flowStoreServiceConnector.deleteSink(id, version);
    }

    // ************************************* find all sinks tests *************************************
    @Test
    public void findAllSinks_sinksRetrieved_returnsSinks() throws FlowStoreServiceConnectorException {
        final SinkContent sinkContentA = new SinkContentBuilder().setName("a").setResource("resource").build();
        final SinkContent sinkContentB = new SinkContentBuilder().setName("b").setResource("resource").build();
        final Sink expectedSinkResultA = new SinkBuilder().setContent(sinkContentA).build();
        final Sink expectedSinkResultB = new SinkBuilder().setContent(sinkContentB).build();

        final List<Sink> expected = new ArrayList<>();
        expected.add(expectedSinkResultA);
        expected.add(expectedSinkResultB);

        assertThat(findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected),
                is(expected));
    }

    @Test
    public void findAllSinks_noResults() throws FlowStoreServiceConnectorException {
        assertThat(findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), Collections.emptyList()),
                is(Collections.emptyList()));
    }

    @Test
    public void findAllSinks_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findAllSinks_nullEntityInResponse() throws FlowStoreServiceConnectorException {
        assertThat(() -> findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private List<Sink> findAllSinks_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient.getHttpClient())
                    .withBaseUrl(FLOW_STORE_URL)
                    .withPathElements(FlowStoreServiceConstants.SINKS);

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findAllSinks();
    }
}
