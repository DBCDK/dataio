package dk.dbc.dataio.jobstore.service.rs;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusBeanTest {
    private StatusBean statusBean;
    private JSONBContext jsonbContext;
    private Query query;
    private FlowStoreServiceConnector flowStoreServiceConnector;


    @BeforeEach
    public void setup() throws URISyntaxException {
        jsonbContext = new JSONBContext();
        query = mock(Query.class);
        initializeStatusBean();
        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(statusBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    // ************************************* getSinkStatusList() tests **********************************************************

    @Test
    public void getSinkStatusList_noSinksFound_returnsStatusOkResponseWithEmptyList() throws JSONBException, FlowStoreServiceConnectorException {
        Response response = statusBean.getSinkStatusList();
        assertOkResponse(response);
        List<SinkStatusSnapshot> sinkStatusSnapshots = jsonbContext.unmarshall((String) response.getEntity(), getSinkStatusSnapshotListType());
        assertThat("ItemInfoSnapshots", sinkStatusSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots is empty", sinkStatusSnapshots.isEmpty(), is(true));
    }

    @Test
    public void getSinkStatusList_sinksFound_returnsStatusOkResponseWithSinkStatusSnapshotList() throws JSONBException, FlowStoreServiceConnectorException {
        Sink sink = new SinkBuilder().setId(1).build();
        SinkStatusSnapshot expectedSinkStatusSnapshot = new SinkStatusSnapshot()
                .withName(sink.getContent().getName())
                .withSinkType(sink.getContent().getSinkType()).withNumberOfJobs(1).withNumberOfChunks(2);

        when(flowStoreServiceConnector.getSink(1)).thenReturn(sink);
        when(flowStoreServiceConnector.findAllSinks()).thenReturn(Collections.singletonList(sink));
        when(statusBean.dependencyTrackingService.jobCount(1)).thenReturn(new Integer[]{1, 2});

        Response response = statusBean.getSinkStatusList();
        assertOkResponse(response);

        List<SinkStatusSnapshot> sinkStatusSnapshots = jsonbContext.unmarshall((String) response.getEntity(), getSinkStatusSnapshotListType());
        assertThat("SinkStatusSnapshots", sinkStatusSnapshots, is(notNullValue()));
        assertThat("SinkStatusSnapshots size", sinkStatusSnapshots.size(), is(1));
        assertThat("SinkStatusSnapshots element", sinkStatusSnapshots.get(0), is(expectedSinkStatusSnapshot));
    }

    // ***************************************** getSinkStatus() tests ***********************************************************

    @Test
    public void getSinkStatus_dependencyTrackingEntityFound_returnsStatusOkResponseWithSinkStatusSnapshot() throws JSONBException, FlowStoreServiceConnectorException {
        Sink sink = new SinkBuilder().setId(1).build();
        SinkStatusSnapshot expectedSinkStatusSnapshot = new SinkStatusSnapshot()
                .withName(sink.getContent().getName())
                .withSinkType(sink.getContent().getSinkType()).withNumberOfJobs(1).withNumberOfChunks(2);

        when(flowStoreServiceConnector.getSink(1)).thenReturn(sink);
        when(statusBean.dependencyTrackingService.jobCount(1)).thenReturn(new Integer[]{1, 2});

        Response response = statusBean.getSinkStatus(1);
        assertOkResponse(response);

        SinkStatusSnapshot sinkStatusSnapshot = jsonbContext.unmarshall((String) response.getEntity(), SinkStatusSnapshot.class);
        assertThat("SinkStatusSnapshots", sinkStatusSnapshot, is(notNullValue()));
        assertThat("SinkStatusSnapshots element", sinkStatusSnapshot, is(expectedSinkStatusSnapshot));
    }

    @Test
    public void getSinkStatus_sinkNotFound_returnsStatusNotFoundResponse() throws FlowStoreServiceConnectorException, JSONBException {
        when(flowStoreServiceConnector.getSink(1))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("not found", Response.Status.NOT_FOUND.getStatusCode()));

        Response response = statusBean.getSinkStatus(1);
        assertNotFoundResponse(response);
    }

    /*
     * Private methods
     */

    private void initializeStatusBean() {
        statusBean = new StatusBean();
        statusBean.jsonbContext = new JSONBContext();
        statusBean.dependencyTrackingService = mock(DependencyTrackingService.class);
        statusBean.entityManager = mock(EntityManager.class);
        statusBean.flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    }

    private void assertOkResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
    }

    private void assertNotFoundResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(false));
    }

    private CollectionType getSinkStatusSnapshotListType() {
        return jsonbContext.getTypeFactory().constructCollectionType(List.class, SinkStatusSnapshot.class);
    }
}
