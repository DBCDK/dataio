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
import static dk.dbc.dataio.integrationtest.ITUtil.clearDbTables;
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
     * When: valid JSON is POSTed to the sinks path
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createSink_ok() {
        // When...
        final String sinkContent = new ITUtil.SinkContentJsonBuilder().build();
        final Response response = HttpClient.doPostWithJson(restClient, sinkContent, baseUrl, FlowStoreServiceEntryPoint.SINKS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));


    }
}
