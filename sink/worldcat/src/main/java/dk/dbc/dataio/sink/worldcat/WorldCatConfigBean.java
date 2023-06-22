package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class WorldCatConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCatConfigBean.class);

    FlowStoreServiceConnector flowStoreServiceConnector;

    private long highestVersionSeen = 0;
    private WorldCatSinkConfig config;

    static class VersionedSink {
        private final Long id;
        private final Long version;
        public  VersionedSink(Long id, Long version) {
            this.id = id;
            this.version = version;
        }

        public Long getVersion() {
            return version;
        }
        public Long getId() {
            return this.id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VersionedSink that = (VersionedSink) o;
            return Objects.equals(id, that.id) && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, version);
        }
    }
    ConcurrentHashMap<VersionedSink, Sink> cache = new ConcurrentHashMap<>();
    public WorldCatConfigBean(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }
    public WorldCatConfigBean() {}
    public WorldCatSinkConfig getConfig(ConsumedMessage consumedMessage) throws SinkException {
        refreshConfig(consumedMessage);
        return config;
    }

    static class FlowstoreConnectorConnectionException extends RuntimeException {
        FlowstoreConnectorConnectionException(Exception e) {
            super(e);
        }
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
                Sink sink = cache.computeIfAbsent(new VersionedSink(sinkId, sinkVersion), versionedSink -> {
                    try {
                        return flowStoreServiceConnector.getSink(versionedSink.getId());
                    } catch (FlowStoreServiceConnectorException e) {
                        throw new FlowstoreConnectorConnectionException(e);
                    }
                });
                config = (WorldCatSinkConfig) sink.getContent().getSinkConfig();
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();
            }
        } catch (FlowstoreConnectorConnectionException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}
