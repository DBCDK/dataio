package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
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
     * Pushes updated config to the flow-store
     *
     * @param config updated config
     * @return updated config
     * @throws HarvesterException on failure to update flow-store
     */
    PeriodicJobsHarvesterConfig push(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (FlowStoreServiceConnectorException | RuntimeException e) {
            // Handle concurrency conflicts
            if (e instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException
                    && ((FlowStoreServiceConnectorUnexpectedStatusCodeException) e)
                    .getStatusCode() == 409) {
                try {
                    final PeriodicJobsHarvesterConfig refreshedConfig =
                            flowStoreServiceConnector.getHarvesterConfig(
                                    config.getId(), PeriodicJobsHarvesterConfig.class);

                    refreshedConfig.getContent().withTimeOfLastHarvest(
                            config.getContent().getTimeOfLastHarvest());

                    push(refreshedConfig);
                    return refreshedConfig;
                } catch (FlowStoreServiceConnectorException fssce) {
                    LOGGER.error("Error refreshing config {}", config.getId(), fssce);
                }
            }
            throw new HarvesterException("Failed to update harvester config: " + config.toString(), e);
        }
    }
}
