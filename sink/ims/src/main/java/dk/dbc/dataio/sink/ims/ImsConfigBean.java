package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the the IMS service sink
 */
@Singleton
public class ImsConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsConfigBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    private long highestVersionSeen = 0;
    private ImsSinkConfig config;

    public ImsSinkConfig getConfig(ConsumedMessage consumedMessage) throws SinkException {
        refreshConfig(consumedMessage);
        return config;
    }

    /**
     * Refreshes the sink config contained in this bean by flow-store lookup if it is outdated
     *
     * @param consumedMessage consumed message containing the version and the id of the sink
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void refreshConfig(ConsumedMessage consumedMessage) throws SinkException {
        try {
            final long sinkId = consumedMessage.getHeaderValue(JmsConstants.SINK_ID_PROPERTY_NAME, Long.class);
            final long sinkVersion = consumedMessage.getHeaderValue(JmsConstants.SINK_VERSION_PROPERTY_NAME, Long.class);
            if (sinkVersion > highestVersionSeen) {
                final Sink sink = flowStoreServiceConnectorBean.getConnector().getSink(sinkId);
                config = (ImsSinkConfig) sink.getContent().getSinkConfig();
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}
