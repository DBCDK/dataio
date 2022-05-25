package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.sink.types.SinkException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the the Update service sink
 */
@Singleton
public class OpenUpdateConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateConfigBean.class);

    @Inject
    @ConfigProperty(name = "UPDATE_VALIDATE_ONLY_FLAG", defaultValue = "false")
    boolean validateOnly;

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    private long highestVersionSeen = 0;
    private OpenUpdateSinkConfig config;

    public OpenUpdateSinkConfig getConfig(ConsumedMessage consumedMessage) throws SinkException {
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
            throw new SinkException(e.getMessage(), e);
        }
    }
}
