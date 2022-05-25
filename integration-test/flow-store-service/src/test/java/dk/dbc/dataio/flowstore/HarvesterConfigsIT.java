package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class HarvesterConfigsIT extends AbstractFlowStoreServiceContainerTest {
    @BeforeClass
    public static void loadInitialState() {
        final URL resource = HarvesterConfigsIT.class.getResource("/initial_state.sql");
        try {
            JDBCUtil.executeScript(flowStoreDbConnection,
                    new File(resource.toURI()), StandardCharsets.UTF_8.name());
        } catch (IOException | URISyntaxException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Given   : a deployed flow-store service
     * When    : valid JSON is POSTed to the harvester configs type path with type as OaiHarvesterConfig
     * Then    : assert that the harvester config created contains the expected information and is of type: OaiHarvesterConfig
     * And     : assert that one harvester config of type RRHarvesterConfig exist in the underlying database
     */
    @Test
    public void createHarvesterConfig_ok() throws FlowStoreServiceConnectorException {
        final OaiHarvesterConfig.Content content = new OaiHarvesterConfig.Content();

        // When...
        OaiHarvesterConfig config = flowStoreServiceConnector
                .createHarvesterConfig(content, OaiHarvesterConfig.class);

        // Then...
        assertThat(config.getId(), is(notNullValue()));
        assertThat(config.getVersion(), is(notNullValue()));
        assertThat(config.getContent(), is(content));
        assertThat(config.getType(), is(OaiHarvesterConfig.class.getName()));
        assertThat(flowStoreServiceConnector.getHarvesterConfig(config.getId(), OaiHarvesterConfig.class),
                is(notNullValue()));
    }

    @Test
    public void deleteHarvesterConfig() throws FlowStoreServiceConnectorException {
        final OaiHarvesterConfig.Content content = new OaiHarvesterConfig.Content();
        OaiHarvesterConfig config = flowStoreServiceConnector.createHarvesterConfig(content, OaiHarvesterConfig.class);

        flowStoreServiceConnector.deleteHarvesterConfig(config.getId(), config.getVersion());

        assertThat(() -> flowStoreServiceConnector.getHarvesterConfig(config.getId(), OaiHarvesterConfig.class),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    /**
     * Given: a deployed flow-store service where a valid harvester config is stored
     * When : valid JSON is POSTed to the harvester config path with an identifier (update) with a change to the harvester
     *        config type
     * Then : assert the harvester config has been updated correctly
     */
    @Test
    public void updateHarvesterConfig_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final OaiHarvesterConfig originalHarvesterConfig = flowStoreServiceConnector.createHarvesterConfig(
                new OaiHarvesterConfig.Content(), OaiHarvesterConfig.class);

        final OaiHarvesterConfig.Content modifiedContent = new OaiHarvesterConfig.Content()
                .withDestination("HarvesterConfigsIT.updateHarvesterConfig_ok");

        final OaiHarvesterConfig modifiedConfig = new OaiHarvesterConfig(
                originalHarvesterConfig.getId(),
                originalHarvesterConfig.getVersion(),
                modifiedContent
        );

        // When...
        OaiHarvesterConfig updatedHarvesterConfig = flowStoreServiceConnector.updateHarvesterConfig(modifiedConfig);

        // Then...
        assertThat(updatedHarvesterConfig.getType(), is(OaiHarvesterConfig.class.getName()));
        assertThat(updatedHarvesterConfig.getVersion(), is(originalHarvesterConfig.getVersion() + 1));
        assertThat(updatedHarvesterConfig.getContent(), is(modifiedContent));
    }

    /**
     * Given: a deployed flow-store service where a harvester config has been stored and updated
     * When : attempting to update the stored harvester config with an outdated version
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void updateHarvesterConfig_wrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final OaiHarvesterConfig harvesterConfig = flowStoreServiceConnector
                .createHarvesterConfig(new OaiHarvesterConfig.Content(), OaiHarvesterConfig.class);

        final OaiHarvesterConfig firstModifiedHarvesterConfig = new OaiHarvesterConfig(
                harvesterConfig.getId(),
                harvesterConfig.getVersion(),
                new OaiHarvesterConfig.Content().withFormat("someFormat")
        );

        final OaiHarvesterConfig secondModifiedHarvesterConfig = new OaiHarvesterConfig(
                harvesterConfig.getId(),
                harvesterConfig.getVersion(),
                new OaiHarvesterConfig.Content().withFormat("someOtherFormat")
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
        }
    }

    @Test
    public void findHarvesterConfigsByType() throws FlowStoreServiceConnectorException {
        final List<RRHarvesterConfig> configs = flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class);
        assertThat(configs.size(), is(6));
    }

    @Test
    public void findEnabledHarvesterConfigsByType() throws FlowStoreServiceConnectorException {
        final List<RRHarvesterConfig> configs = flowStoreServiceConnector.findEnabledHarvesterConfigsByType(RRHarvesterConfig.class);
        assertThat(configs.size(), is(4));
    }
}
