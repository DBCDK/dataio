package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.createFlowBinder;
import static dk.dbc.dataio.integrationtest.ITUtil.createSubmitter;
import static dk.dbc.dataio.integrationtest.ITUtil.doPostWithJson;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flow binders collection part of the flow store service
 */
public class FlowBindersIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        restClient = ClientBuilder.newClient();
        dbConnection = newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearAllDbTables(dbConnection);
    }

     /**
     * Given: a deployed flow-store service with a submitter
     * When: valid JSON is POSTed to the flow binders path referencing the submitter
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createFlowBinder_ok() throws Exception {
        // Given...
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmittersIT.SubmitterContentJsonBuilder().build());

        // When...
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = doPostWithJson(restClient, flowBinderContent, baseUrl, ITUtil.FLOW_BINDERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.FLOW_BINDERS_TABLE_SELECT_CONTENT_STMT, id);

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowBinderContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the flow binders path causes JsonException
     * Then: request returns with a NOT ACCEPTED http status code
     */
    @Test
    public void createFlowBinder_errorWhenJsonExceptionIsThrown() {
        // When...
        final Response response = doPostWithJson(restClient, "<invalid json />", baseUrl, ITUtil.SUBMITTERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with the same name
     * Then: request returns with a CONFLICT http status code
     */
    @Test
    public void createFlowBinder_duplicateName() throws Exception {
        // Note that we set different destinations to ensure we don't risk matching search keys.

        // Given...
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmittersIT.SubmitterContentJsonBuilder().build());

        final String name = "createFlowBinder_duplicateName";
        final String firstFlowBinderContent = new FlowBinderContentJsonBuilder()
                .setName(name)
                .setDestination("base1")
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        createFlowBinder(restClient, baseUrl, firstFlowBinderContent);

        // When...
        final String secondFlowBinderContent = new FlowBinderContentJsonBuilder()
                .setName(name)
                .setDestination("base2")
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = doPostWithJson(restClient, secondFlowBinderContent, baseUrl, ITUtil.FLOW_BINDERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: adding flow binder which references non-existing submitter
     * Then: request returns with a GONE http status code
     */
    @Test
    public void createFlowBinder_referencedSubmitterNotFound() throws Exception {
        // When...
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setSubmitterIds(Arrays.asList(123456789L))
                .build();
        final Response response = doPostWithJson(restClient, flowBinderContent, baseUrl, ITUtil.FLOW_BINDERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.GONE.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with different name but matching search key
     * Then: request returns with a CONFLICT http status code
     */
    @Test
    public void createFlowBinder_searchKeyExistsInSearchIndex() throws Exception {
        // Given...
        final long submitterId = createSubmitter(restClient, baseUrl,
                new SubmittersIT.SubmitterContentJsonBuilder().build());

        String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_1")
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();
        createFlowBinder(restClient, baseUrl, flowBinderContent);

        // When...
        flowBinderContent = new FlowBinderContentJsonBuilder()
                .setName("createFlowBinder_searchKeyExistsInSearchIndex_2")
                .setSubmitterIds(Arrays.asList(submitterId))
                .build();

        final Response response = doPostWithJson(restClient, flowBinderContent, baseUrl, ITUtil.FLOW_BINDERS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
    }

    public static class FlowBinderContentJsonBuilder extends ITUtil.JsonBuilder {
        private String name = "name";
        private String packaging = "packaging";
        private String format = "format";
        private String destination = "destination";
        private String charset = "charset";
        private String description = "description";
        private String recordSplitter = "recordSplitter";
        private Long flowId = 42L;
        private List<Long> submitterIds = new ArrayList<>(Arrays.asList(43L));

        public FlowBinderContentJsonBuilder setCharset(String charset) {
            this.charset = charset;
            return this;
        }

        public FlowBinderContentJsonBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public FlowBinderContentJsonBuilder setDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public FlowBinderContentJsonBuilder setFlowId(Long flowId) {
            this.flowId = flowId;
            return this;
        }

        public FlowBinderContentJsonBuilder setFormat(String format) {
            this.format = format;
            return this;
        }

        public FlowBinderContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public FlowBinderContentJsonBuilder setPackaging(String packaging) {
            this.packaging = packaging;
            return this;
        }

        public FlowBinderContentJsonBuilder setRecordSplitter(String recordSplitter) {
            this.recordSplitter = recordSplitter;
            return this;
        }

        public FlowBinderContentJsonBuilder setSubmitterIds(List<Long> submitterIds) {
            this.submitterIds = new ArrayList<>(submitterIds);
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("packaging", packaging)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("format", format)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("charset", charset)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("destination", destination)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("flowId", flowId)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongArray("submitterIds", submitterIds));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }
}
