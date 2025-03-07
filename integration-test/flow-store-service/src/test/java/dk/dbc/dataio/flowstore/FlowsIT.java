package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the flows collection part of the flow store service
 */
public class FlowsIT extends AbstractFlowStoreServiceContainerTest {
    @Test
    public void flowViewSer() throws JSONBException {
        String value = "{\"id\":1201,\"version\":1,\"name\":\"Publizon2Dmat\",\"description\":\"Something wonderfully descriptive\",\"components\":[]}";
        jsonbContext.unmarshall(value, FlowView.class);
    }

    @Test
    public void createAndUpdateJsarFlow() throws URISyntaxException, IOException, FlowStoreServiceConnectorException {
        byte[] jsar = readFile("publizon-dmat.jsar");
        FlowView flow = flowStoreServiceConnector.createFlow(Instant.now().toEpochMilli(), jsar);
        byte[] jsarResult = flowStoreServiceConnector.getJsar(flow.getId());
        assertEquals("Publizon2Dmat", flow.getName());
        assertEquals(1, flow.getVersion());
        assertEquals("Something wonderfully descriptive", flow.getDescription());
        assertArrayEquals("Downloaded jsar should be identical to the local one", jsar, jsarResult);
        byte[] jsar2 = readFile("publizon-dmat-modified.jsar");
        FlowView flow2 = flowStoreServiceConnector.updateFlow(flow.getId(), Instant.now().toEpochMilli(), jsar2);
        assertEquals("Publizon2Dmat", flow2.getName());
        assertEquals(2, flow2.getVersion());
        assertEquals("Something even more wonderfully descriptive", flow2.getDescription());
        byte[] jsar2Result = flowStoreServiceConnector.getJsar(flow.getId());
        assertArrayEquals("Downloaded jsar should be identical to the local one", jsar2, jsar2Result);
    }

    private byte[] readFile(String name) throws URISyntaxException, IOException {
        URI uri = getClass().getClassLoader().getResource(name).toURI();
        return Files.readAllBytes(Path.of(uri));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path without an identifier
     * Then : a flow it created and returned
     * And  : assert that the flow created contains the same information as the flowContent given as input
     * And  : the flow view is updated
     */
    @Test
    public void createFlow_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.createFlow_ok")
                .build();

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // And...
        assertThat(flow.getContent(), is(content));

        // And...
        final FlowView flowView = getFlowView(flow.getId());
        assertThat("flow view version", flowView.getVersion(), is(1L));
        assertThat("flow view name", flowView.getName(), is(content.getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the flows path
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createFlow_invalidJson_BadRequest() {
        new HttpPost(HttpClient.create(flowStoreServiceConnector.getClient()))
                .withBaseUrl(flowStoreServiceBaseUrl)
                .withPathElements(FlowStoreServiceConstants.FLOWS)
                .withJsonData("<invalid json />")
                .executeAndExpect(Response.Status.BAD_REQUEST)
                .close();
    }

    /**
     * Given: a deployed flow-store service containing flow resource
     * When : adding flow with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlow_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent flowContent = new FlowContentBuilder()
                .setName("FlowsIT.createFlow_duplicateName_NotAcceptable")
                .build();

        try {
            flowStoreServiceConnector.createFlow(flowContent);
            // When...
            flowStoreServiceConnector.createFlow(flowContent);
            fail("Primary key violation was not detected as input to createFlow().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a flow with an unknown flow id
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getFlow_WrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        try {
            // When...
            flowStoreServiceConnector.getFlow(new Date().getTime());

            fail("Invalid request to getFlow() was not detected.");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with a valid identifier
     * Then : a flow is found and returned
     * And  : assert that the flow found contains the same information as the flow created
     */
    @Test
    public void getFlow_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.getFlow_ok")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);

        // Then...
        Flow flowToGet = flowStoreServiceConnector.getFlow(flow.getId());

        // And...
        assertThat(flowToGet.getContent(), is(content));
    }

    /**
     * Given: a deployed flow-store service containing three flows
     * When: GETing flows collection
     * Then: request returns with 3 flows
     * And: the flows are sorted alphabetically by name
     */
    @Test
    public void findAllFlows_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent contentA = new FlowContentBuilder()
                .setName("a_FlowsIT.findAllFlows_ok")
                .build();
        final FlowContent contentB = new FlowContentBuilder()
                .setName("b_FlowsIT.findAllFlows_ok")
                .build();
        final FlowContent contentC = new FlowContentBuilder()
                .setName("c_FlowsIT.findAllFlows_ok")
                .build();

        Flow flowSortsThird = flowStoreServiceConnector.createFlow(contentC);
        Flow flowSortsFirst = flowStoreServiceConnector.createFlow(contentA);
        Flow flowSortsSecond = flowStoreServiceConnector.createFlow(contentB);

        // When...
        List<FlowView> listOfFlowViews = flowStoreServiceConnector.findAllFlows();

        // Then...
        assertThat(listOfFlowViews.size() >= 3, is(true));

        // And...
        assertThat(listOfFlowViews.get(0).getName(),
                is(flowSortsFirst.getContent().getName()));
        assertThat(listOfFlowViews.get(1).getName(),
                is(flowSortsSecond.getContent().getName()));
        assertThat(listOfFlowViews.get(2).getName(),
                is(flowSortsThird.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with a valid identifier
     * Then : a flow is found and returned
     * And  : assert that the flow found has an id, a version and contains the same information as the flow created
     */
    @Test
    public void findFlowByName_ok() throws FlowStoreServiceConnectorException {
        // When...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.findFlowByName_ok")
                .build();

        // Then...
        Flow flow = flowStoreServiceConnector.createFlow(content);
        Flow flowToGet = flowStoreServiceConnector.findFlowByName(content.getName());

        // And...
        assertNotNull(flowToGet);
        assertThat(flowToGet.getContent(), is(content));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an none existing identifier
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void findFlowByName_notFound() throws FlowStoreServiceConnectorException {
        try {
            // When...
            flowStoreServiceConnector.findFlowByName("FlowsIT.findFlowByName_notFound");

            fail("Invalid request to findFlowByName() was not detected");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /*
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flows path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flows exist in the underlying database
     */
    @Test
    public void updateFlow_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException, URISyntaxException, IOException {
        // Given...
        try {
            byte[] jsar = readFile("publizon-dmat.jsar");
            // When...
            final FlowContent content = new FlowContentBuilder()
                    .setName("FlowsIT.updateFlow_wrongIdNumber_NotFound")
                    .build();
            flowStoreServiceConnector.updateFlow(666, Instant.now().toEpochMilli(), jsar);

            fail("Wrong flow Id was not detected as input to updateFlow()");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
        }
    }

    /**
     * Given: a deployed flow-store service and a none referenced flow is stored
     * When : attempting to delete the flow
     * Then : the flow is deleted
     */
    @Test
    public void deleteFlow_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final FlowContent flowContent = new FlowContentBuilder()
                .setName("FlowsIT.deleteFlow_ok")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(flowContent);

        // When...
        flowStoreServiceConnector.deleteFlow(flow.getId(), flow.getVersion());

        // Then... Verify that the flow is deleted
        try {
            flowStoreServiceConnector.getFlow(flow.getId());
            fail("Flow was not deleted");
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to delete a flow that does not exist
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void deleteFlow_noFlowToDelete() {
        // Given...
        final long nonExistingFlowId = new Date().getTime();

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(nonExistingFlowId, 1L);
            fail("None existing flow was not detected");

            // Then ...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service and a none referenced flow is stored.
     * And  : the flow is updated and, valid JSON is POSTed to the flows path with an identifier (update)
     * and correct version number
     * When : attempting to delete the flow with the previous version number, valid JSON is POSTed to the flows
     * path with an identifier (delete) and wrong version number
     * <p>
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void deleteFlow_optimisticLocking() throws FlowStoreServiceConnectorException, URISyntaxException, IOException {

        // Given...
        final FlowContent content = new FlowContentBuilder()
                .setName("FlowsIT.deleteFlow_optimisticLocking")
                .build();
        Flow flow = flowStoreServiceConnector.createFlow(content);
        long versionFirst = flow.getVersion();
        long versionSecond = versionFirst + 1;

        // And
        final FlowContent updatedContent = new FlowContentBuilder()
                .setName(content.getName())
                .setDescription("updated")
                .build();
        byte[] jsar = readFile("publizon-dmat.jsar");
        final FlowView updatedFlow = flowStoreServiceConnector.updateFlow(flow.getId(), Instant.now().toEpochMilli(), jsar);
        assertThat(updatedFlow.getVersion(), is(versionSecond));

        // Verify before delete
        Flow flowBeforeDelete = flowStoreServiceConnector.getFlow(flow.getId());
        assertThat(flowBeforeDelete.getId(), is(flow.getId()));
        assertThat(flowBeforeDelete.getVersion(), is(versionSecond));

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(flow.getId(), versionFirst);
            fail("Flow was deleted");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
            flowStoreServiceConnector.deleteFlow(flow.getId(), versionSecond);
        }
    }

    /**
     * Given: a deployed flow-store service and a flow referenced by a flow binder is stored.
     * When : attempting to delete the referenced flow
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void deleteFlow_flowBinderExists_Conflict() throws FlowStoreServiceConnectorException {
        // Given
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .setNumber(new Date().getTime())
                .build());

        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName("FlowsIT.deleteFlow_flowBinderExists_Conflict")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        try {
            // When...
            flowStoreServiceConnector.deleteFlow(flow.getId(), flow.getVersion());
            fail("Flow was deleted");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }
    }

    private FlowView getFlowView(long flowId) {
        try (PreparedStatement stmt = JDBCUtil.query(flowStoreDbConnection,
                "SELECT view FROM flows WHERE id=?", flowId)) {
            final ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            return new JSONBContext().unmarshall(resultSet.getString(1), FlowView.class);
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
