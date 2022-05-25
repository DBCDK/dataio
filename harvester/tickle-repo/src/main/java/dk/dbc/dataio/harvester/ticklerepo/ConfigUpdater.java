package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUpdater.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector;

    static ConfigUpdater create(FlowStoreServiceConnector flowStoreServiceConnector) {
        return new ConfigUpdater(flowStoreServiceConnector);
    }

    private ConfigUpdater(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    /**
     * Sets lastBatchHarvested field of given config to that of given batch
     * and pushes the updated config to the flow-store
     * @param config config to updated
     * @param batch batch to update config with
     * @throws HarvesterException on failure to update flow-store
     */
    void updateHarvesterConfig(TickleRepoHarvesterConfig config, Batch batch) throws HarvesterException {
        if (batch != null) {
            config.getContent().withLastBatchHarvested(batch.getId());
            updateHarvesterConfig(config);
        }
    }

    private TickleRepoHarvesterConfig updateHarvesterConfig(TickleRepoHarvesterConfig config) throws HarvesterException {
        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            // Handle concurrency conflicts
            if (e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException
                    && ((FlowStoreServiceConnectorUnexpectedStatusCodeException) e).getStatusCode() == 409) {
                try {
                    final TickleRepoHarvesterConfig refreshedConfig = flowStoreServiceConnector
                            .getHarvesterConfig(config.getId(), TickleRepoHarvesterConfig.class);
                    refreshedConfig.getContent()
                            .withLastBatchHarvested(config.getContent().getLastBatchHarvested());
                    return updateHarvesterConfig(refreshedConfig);
                } catch (FlowStoreServiceConnectorException fssce) {
                    LOGGER.error("Error refreshing config " + config.getId(), fssce);
                }
            }
            throw new HarvesterException("Failed to update harvester config: " + config.toString(), e);
        }
    }
}
