package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the IMS service sink
 */
public class ImsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsConfig.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    private long highestVersionSeen = 0;
    private ImsSinkConfig config;

    public ImsConfig(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    public synchronized ImsSinkConfig getConfig(ConsumedMessage consumedMessage) {
        refreshConfig(consumedMessage);
        return config;
    }

    /**
     * Refreshes the sink config contained in this bean by flow-store lookup if it is outdated
     *
     * @param consumedMessage consumed message containing the version and the id of the sink
     */
    private void refreshConfig(ConsumedMessage consumedMessage) {
        try {
            long sinkId = JMSHeader.sinkId.getHeader(consumedMessage, Long.class);
            long sinkVersion = JMSHeader.sinkVersion.getHeader(consumedMessage, Long.class);
            if (sinkVersion > highestVersionSeen) {
                Sink sink = flowStoreServiceConnector.getSink(sinkId);
                config = (ImsSinkConfig) sink.getContent().getSinkConfig();
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
