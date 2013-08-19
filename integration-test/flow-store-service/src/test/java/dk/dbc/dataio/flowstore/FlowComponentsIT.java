package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.flowstore.TestUtil.FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT;
import static dk.dbc.dataio.flowstore.TestUtil.clearDbTables;
import static dk.dbc.dataio.flowstore.TestUtil.doPostWithJson;
import static dk.dbc.dataio.flowstore.TestUtil.getResourceIdentifierFromLocationHeaderAndAssertHasValue;
import static dk.dbc.dataio.flowstore.TestUtil.newDbConnection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the flow components collection part of the flow store service
 */
public class FlowComponentsIT {
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
        clearDbTables(dbConnection, TestUtil.FLOW_COMPONENTS_TABLE_NAME);
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
        final Response response = doPostWithJson(restClient, flowComponentContent, baseUrl, TestUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // And ...
        final TestUtil.ResourceIdentifier resId = getResourceIdentifierFromLocationHeaderAndAssertHasValue(response);

        // And ...
        final List<List<Object>> rs = JDBCUtil.queryForRowLists(dbConnection, FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT,
                resId.getId(), new Date(resId.getVersion()));

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
        final Response response = doPostWithJson(restClient, "<invalid json />", baseUrl, TestUtil.FLOW_COMPONENTS_URL_PATH);

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
        final Response response = doPostWithJson(restClient, null, baseUrl, TestUtil.FLOW_COMPONENTS_URL_PATH);

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
        final Response response = doPostWithJson(restClient, "", baseUrl, TestUtil.FLOW_COMPONENTS_URL_PATH);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }
}
