package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flow components collection part of the flow store service
 */
public class FlowComponentsIT {
    private static final String FLOW_COMPONENTS_TABLE_NAME = "flow_components";
    private static final String COMPONENTS_URL_PATH = "components";

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
        clearDbTables(dbConnection, FLOW_COMPONENTS_TABLE_NAME);
    }

    /**
     * Given: a deployed flow-store service
     * When: valid JSON is POSTed to the components path
     * Then: request returns with a CREATED http status code
     * And: request returns with a Location header pointing to the newly created resource
     * And: posted data can be found in the underlying database
     */
    @Test
    public void createComponent_Ok() throws SQLException {
        // When...

        final String flowComponentContent = "{\"name\": \"testComponentName\"}";
        final Response response = doPost(flowComponentContent);

        // Then...

        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...

        final String[] locationHeaderValueParts = ((String) response.getHeaders().get("Location").get(0)).split("/");
        final String createdId = locationHeaderValueParts[locationHeaderValueParts.length - 2];
        final String createdVersion = locationHeaderValueParts[locationHeaderValueParts.length - 1];

        // And ...

        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection,
                String.format("SELECT content FROM %s WHERE id=? AND version=?", FLOW_COMPONENTS_TABLE_NAME),
                createdId, new Date(Long.valueOf(createdVersion)));

        assertThat(rs.size(), is(1));
        assertThat((String) rs.get(0).get(0), is(flowComponentContent));
    }

    /**
     * Given: a deployed flow-store service
     * When: invalid JSON is POSTed to the components path
     * Then: request returns with a INTERNAL SERVER ERROR http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenInvalidJson() {
        // When...

        final Response response = doPost("<invalid json />");

        // Then...

        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: null value is POSTed to the components path
     * Then: request returns with a INTERNAL SERVER ERROR http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenNull() {
        // When...

        final Response response = doPost(null);

        // Then...

        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When: empty value is POSTed to the components path
     * Then: request returns with a INTERNAL SERVER ERROR http status code
     */
    @Test
    public void createComponent_ErrorWhenGivenEmpty() {
        // When...

        final Response response = doPost("");

        // Then...

        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    private static Connection newDbConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(
                String.format("jdbc:h2:tcp://localhost:%s/mem:flow_store", System.getProperty("h2.port")),
                "root", "root");
        conn.setAutoCommit(true);
        return conn;
    }

    private static void clearDbTables(Connection conn, String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            JDBCUtil.update(conn, String.format("DELETE FROM %s", tableName));
        }
    }

    private static Response doPost(String data) {
        final WebTarget target = restClient.target(baseUrl).path(COMPONENTS_URL_PATH);
        return target.request().post(Entity.entity(data, MediaType.APPLICATION_JSON));
    }

}
