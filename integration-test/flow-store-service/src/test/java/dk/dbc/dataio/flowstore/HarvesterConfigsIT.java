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

package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.jsonb.JSONBException;
import net.jodah.failsafe.RetryPolicy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HarvesterConfigsIT {
    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;
    private static FlowStoreServiceConnector flowStoreServiceConnector;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        dbConnection = newIntegrationTestConnection("flowstore");
        restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(
            FailSafeHttpClient.create(restClient, new RetryPolicy().withMaxRetries(0)),
            baseUrl);
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
     * Given   : a deployed flow-store service
     * When    : valid JSON is POSTed to the harvester configs type path with type as RRHarvesterConfig
     * Then    : assert that the harvester config created contains the expected information and is of type: RRHarvesterConfig
     * And     : assert that one harvester config of type RRHarvesterConfig exist in the underlying database
     */
    @Test
    public void createHarvesterConfig_ok() throws Exception{

        final RRHarvesterConfig.Content newRRConfigContent = new RRHarvesterConfig.Content();

        // When...
        RRHarvesterConfig newRRHarvesterConfig = flowStoreServiceConnector.createHarvesterConfig(newRRConfigContent, RRHarvesterConfig.class);

        // Then...
        assertThat(newRRHarvesterConfig.getId(), is(notNullValue()));
        assertThat(newRRHarvesterConfig.getVersion(), is(notNullValue()));
        assertThat(newRRHarvesterConfig.getContent(), is(newRRConfigContent));
        assertThat(newRRHarvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));
        assertThat(flowStoreServiceConnector.getHarvesterConfig(newRRHarvesterConfig.getId(), RRHarvesterConfig.class), is(notNullValue()));
    }

    @Test
    public void deleteHarvesterConfig() throws Exception {
        loadInitialState();
        assertThat(flowStoreServiceConnector.getHarvesterConfig(1, RRHarvesterConfig.class), is(notNullValue()));
        flowStoreServiceConnector.deleteHarvesterConfig(1, 1);
        assertThat(() -> flowStoreServiceConnector.getHarvesterConfig(1, RRHarvesterConfig.class), isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    /**
     * Given: a deployed flow-store service where a valid harvester config is stored
     * When : valid JSON is POSTed to the harvester config path with an identifier (update) with a change to the harvester
     *        config type
     * Then : assert the harvester config has been updated correctly
     * And  : assert that updated data can be found in the underlying database and only one harvester config of the expected type exists
     */
    @Test
    public void updateHarvesterConfigUpdateType_ok() throws FlowStoreServiceConnectorException, JSONBException {

        // Given...
        final RRHarvesterConfig originalHarvesterConfig = flowStoreServiceConnector.createHarvesterConfig(
                new RRHarvesterConfig.Content(),
                RRHarvesterConfig.class
        );

        final RRHarvesterConfig.Content newConfigContent = new RRHarvesterConfig.Content()
                .withBatchSize(originalHarvesterConfig.getContent().getBatchSize() + 42);

        final RRHarvesterConfig modifiedHarvesterConfig = new RRHarvesterConfig(
                originalHarvesterConfig.getId(),
                originalHarvesterConfig.getVersion(),
                newConfigContent
        );

        // When...
        RRHarvesterConfig updatedHarvesterConfig = flowStoreServiceConnector.updateHarvesterConfig(modifiedHarvesterConfig);

        // Then...
        assertThat(updatedHarvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));
        assertThat(updatedHarvesterConfig.getVersion(), is(originalHarvesterConfig.getVersion() + 1));
        assertThat(updatedHarvesterConfig.getContent(), is(newConfigContent));

        // And...
        assertThat(flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class).size(), is(1));
    }

    /**
     * Given: a deployed flow-store service where a valid harvester config is stored
     * When : valid JSON is POSTed to the harvester config path with an identifier (update)
     * Then : assert the harvester config has been updated correctly
     * And  : assert that updated data can be found in the underlying database and only one harvester config exists
     */
    @Test
    public void updateHarvesterConfig_ok() throws FlowStoreServiceConnectorException, JSONBException {

        final RRHarvesterConfig originalHarvesterConfig = flowStoreServiceConnector.createHarvesterConfig(
                new RRHarvesterConfig.Content(),
                RRHarvesterConfig.class
        );

        final RRHarvesterConfig.Content newConfigContent = new RRHarvesterConfig.Content().withFormat("someFormat");

        final RRHarvesterConfig modifiedHarvesterConfig = new RRHarvesterConfig(
                originalHarvesterConfig.getId(),
                originalHarvesterConfig.getVersion(),
                newConfigContent
        );

        // When...
        RRHarvesterConfig updatedHarvesterConfig = flowStoreServiceConnector.updateHarvesterConfig(modifiedHarvesterConfig);
        assertThat(updatedHarvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));
        assertThat(updatedHarvesterConfig.getVersion(), is(originalHarvesterConfig.getVersion() + 1));
        assertThat(updatedHarvesterConfig.getContent(), is(newConfigContent));

        assertThat(flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class).size(), is(1));
    }

    /**
     * Given: a deployed flow-store service where a harvester config has been stored and updated
     * When : attempting to update the stored harvester config with an outdated version
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     * And  : assert that only one harvester config of the expected type exists in the underlying database
     */
    @Test
    public void updateHarvesterConfig_WrongVersion_Conflict() throws FlowStoreServiceConnectorException, JSONBException {

        // Given...
        // Create harvester config
        final RRHarvesterConfig harvesterConfig = flowStoreServiceConnector.createHarvesterConfig(new RRHarvesterConfig.Content(), RRHarvesterConfig.class);

        final RRHarvesterConfig firstModifiedHarvesterConfig = new RRHarvesterConfig(
                harvesterConfig.getId(),
                harvesterConfig.getVersion(),
                new RRHarvesterConfig.Content().withFormat("someFormat")
        );

        final RRHarvesterConfig secondModifiedHarvesterConfig = new RRHarvesterConfig(
                harvesterConfig.getId(),
                harvesterConfig.getVersion(),
                new RRHarvesterConfig.Content().withFormat("someOtherFormat")
        );

        // update harvester config
        flowStoreServiceConnector.updateHarvesterConfig(firstModifiedHarvesterConfig);

        try {
            // When...
            flowStoreServiceConnector.updateHarvesterConfig(secondModifiedHarvesterConfig);
            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateHarvesterConfig().");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {

            // And...
            assertThat(e.getStatusCode(), is(409));

            // And...
            assertThat(flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class).size(), is(1));
        }
    }

    @Test
    public void findHarvesterConfigsByType() throws FlowStoreServiceConnectorException {
        loadInitialState();
        final List<RRHarvesterConfig> configs = flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class);
        assertThat(configs.size(), is(6));
    }

    @Test
    public void findEnabledHarvesterConfigsByType() throws FlowStoreServiceConnectorException {
        loadInitialState();
        final List<RRHarvesterConfig> configs = flowStoreServiceConnector.findEnabledHarvesterConfigsByType(RRHarvesterConfig.class);
        assertThat(configs.size(), is(4));
    }

    private void loadInitialState() {
        final URL resource = HarvesterConfigsIT.class.getResource("/initial_state.sql");
        try {
            JDBCUtil.executeScript(dbConnection, new File(resource.toURI()), StandardCharsets.UTF_8.name());
        } catch (IOException | URISyntaxException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
