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
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;

import static dk.dbc.dataio.integrationtest.ITUtil.clearAllDbTables;
import static dk.dbc.dataio.integrationtest.ITUtil.newIntegrationTestConnection;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HarvesterConfigsIT {

    private static Client restClient;
    private static Connection dbConnection;
    private static String baseUrl;
    private static FlowStoreServiceConnector flowStoreServiceConnector;
    private static JSONBContext jsonbContext;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException, SQLException {
        baseUrl = ITUtil.FLOW_STORE_BASE_URL;
        dbConnection = newIntegrationTestConnection();
        restClient = HttpClient.newClient();
        flowStoreServiceConnector = new FlowStoreServiceConnector(restClient, baseUrl);
        jsonbContext = new JSONBContext();
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
     * Then    : assert that the Harvester config created contains the expected information and is of type: RRHarvesterConfig
     * And When: valid JSON is POSTed to the harvester configs type path with type as ODLRRHarvesterConfig
     * Then    : assert that the Harvester config created contains the expected information and is of type: OLDRRHarvesterConfig
     */
    @Test
    public void createHarvesterConfig_ok() throws Exception{

        final RRHarvesterConfig.Content newRRConfigContent = new RRHarvesterConfig.Content();
        final OLDRRHarvesterConfig.Content oldRRConfigContent = new OLDRRHarvesterConfig.Content();

        // When...
        HarvesterConfig newRRHarvesterConfig = flowStoreServiceConnector.createHarvesterConfig(newRRConfigContent, RRHarvesterConfig.class);

        // Then...
        assertThat(newRRHarvesterConfig.getId(), is(notNullValue()));
        assertThat(newRRHarvesterConfig.getVersion(), is(notNullValue()));
        assertThat(newRRHarvesterConfig.getContent(), is(newRRConfigContent));
        assertThat(newRRHarvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));

        // And When...
        HarvesterConfig oldRRHarvesterConfig = flowStoreServiceConnector.createHarvesterConfig(oldRRConfigContent, OLDRRHarvesterConfig.class);

        // Then...
        assertThat(oldRRHarvesterConfig.getId(), is(notNullValue()));
        assertThat(oldRRHarvesterConfig.getVersion(), is(notNullValue()));
        assertThat(oldRRHarvesterConfig.getContent(), is(oldRRConfigContent));
        assertThat(oldRRHarvesterConfig.getType(), is(OLDRRHarvesterConfig.class.getName()));
    }

    /**
     * Given: a deployed flow-store service
     * When : invalid JSON is posted to the configs type path
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void createHarvesterConfig_invalidJson_BadRequest() {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind("type", RRHarvesterConfig.class.getName());

        // When...
        final Response response = HttpClient.doPostWithJson(restClient, "<invalid json />", baseUrl, path.build());

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : incompatible content and type is posted to the configs type path
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void createHarvesterConfig_incompatibleContentAndType_BadRequest() throws JSONBException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind("type", HarvesterConfig.class.getName());

        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();

        // When...
        final Response response = HttpClient.doPostWithJson(restClient, jsonbContext.marshall(configContent), baseUrl, path.build());

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }
}
