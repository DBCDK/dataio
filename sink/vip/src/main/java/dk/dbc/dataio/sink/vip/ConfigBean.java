package dk.dbc.dataio.sink.vip;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the VIP service sink
 */
public class ConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnectorBean;

    private long highestVersionSeen = 0;
    private VipSinkConfig config;

    public ConfigBean(FlowStoreServiceConnector flowStoreServiceConnectorBean) {
        this.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
    }

    public synchronized VipSinkConfig getConfig(ConsumedMessage consumedMessage) {
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
                Sink sink = flowStoreServiceConnectorBean.getSink(sinkId);
                config = (VipSinkConfig) sink.getContent().getSinkConfig();
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
