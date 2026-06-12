package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigRefresher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRefresher.class);

    private final boolean validateOnly = SinkConfig.UPDATE_VALIDATE_ONLY_FLAG.asBoolean();
    private final String groupId = SinkConfig.GROUP_ID.asString();
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    private long highestVersionSeen = 0;
    private OpenUpdateSinkConfig config;

    public ConfigRefresher(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    public synchronized OpenUpdateSinkConfig getConfig(ConsumedMessage consumedMessage) {
        refresh(consumedMessage);
        return config;
    }

    private void refresh(ConsumedMessage consumedMessage) {
        try {
            long sinkId = JMSHeader.sinkId.getHeader(consumedMessage, Long.class);
            long sinkVersion = JMSHeader.sinkVersion.getHeader(consumedMessage, Long.class);
            if (sinkVersion > highestVersionSeen) {
                Sink sink = flowStoreServiceConnector.getSink(sinkId);
                config = ((OpenUpdateSinkConfig) sink.getContent().getSinkConfig())
                        .withGroupId(groupId)
                        .withValidateOnly(validateOnly);
                if (!validateOnly) {
                    // ignoredValidationErrors are only meaningful during validation runs,
                    // strip them in normal update mode so the full error set is reported.
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
