package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the the Update service sink
 */
public class OpenUpdateConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateConfig.class);
    boolean validateOnly = SinkConfig.UPDATE_VALIDATE_ONLY_FLAG.asBoolean();
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private long highestVersionSeen = 0;
    private OpenUpdateSinkConfig config;

    public OpenUpdateConfig(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    public synchronized OpenUpdateSinkConfig getConfig(ConsumedMessage consumedMessage) {
        refreshConfig(consumedMessage);
        return config;
    }

    /**
     * Refreshes the sink config contained in this bean by flow-store lookup if it is outdated
     *
     * @param consumedMessage consumed message containing the version and the id of the sink
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void refreshConfig(ConsumedMessage consumedMessage) {
        try {
            long sinkId = JMSHeader.sinkId.getHeader(consumedMessage, Long.class);
            long sinkVersion = JMSHeader.sinkVersion.getHeader(consumedMessage, Long.class);
            if (sinkVersion > highestVersionSeen) {
                Sink sink = flowStoreServiceConnector.getSink(sinkId);
                config = (OpenUpdateSinkConfig) sink.getContent().getSinkConfig();
                if (!validateOnly) {
                    // Ignoring validation errors is only allowed when sink is running
                    // in validate only mode.
                    config.withIgnoredValidationErrors(null);
                }
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException("Unable to retrieve configuration from flowstore", e);
        }
    }
}
