package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.integrationtest.ITUtil;
import java.sql.Connection;
import java.sql.SQLException;
import javax.ws.rs.client.Client;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static dk.dbc.dataio.integrationtest.ITUtil.newDbConnection;
import static dk.dbc.dataio.integrationtest.ITUtil.createSink;
import static dk.dbc.dataio.integrationtest.ITUtil.clearDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.getResourceIdFromLocationHeaderAndAssertHasValue;
import java.util.List;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class SinksIT {

    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        restClient = HttpClient.newClient();
        dbConnection = newDbConnection();
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        JDBCUtil.closeConnection(dbConnection);
    }

    @After
    public void tearDown() throws SQLException {
        clearDbTables(dbConnection, ITUtil.SINKS_TABLE_NAME);
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the sinks path
     * Then : request returns with a CREATED http status code
     * And  : request returns with a Location header pointing to the newly created resource
     * And  : posted data can be found in the underlying database
     */
    @Test
    public void createSink_ok() throws SQLException {
        // When...
        final String sinkContent = new ITUtil.SinkContentJsonBuilder().build();
        final Response response = HttpClient.doPostWithJson(restClient, sinkContent, baseUrl, FlowStoreServiceEntryPoint.SINKS);
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));
        // And ...
        final long id = getResourceIdFromLocationHeaderAndAssertHasValue(response);
        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, ITUtil.SINKS_TABLE_SELECT_CONTENT_STMT, id);
        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(sinkContent));
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the sinks path causes JsonException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void createSink_invalidJson_BadRequest() {
        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, FlowStoreServiceEntryPoint.SINKS);
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing sink resource
     * When : adding sink with the same name
     * Then : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createSink_duplicateName_NotAcceptable() throws Exception {
        // Given...
        final String sinkContent1 = new ITUtil.SinkContentJsonBuilder().build();
        createSink(restClient, baseUrl, sinkContent1);
        // When...
        final String sinkContent2 = new ITUtil.SinkContentJsonBuilder().build();
        final Response response = HttpClient.doPostWithJson(restClient, sinkContent2, baseUrl, FlowStoreServiceEntryPoint.SINKS);
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }
}
