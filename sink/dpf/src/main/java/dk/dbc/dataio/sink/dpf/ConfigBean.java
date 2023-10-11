package dk.dbc.dataio.sink.dpf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the DPF sink
 */
public class ConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final Cache<Long, FlowBinder> cachedFlowBinders = CacheBuilder.newBuilder().maximumSize(10).expireAfterAccess(Duration.ofHours(1)).build();
    private long highestVersionSeen = 0;
    private DpfSinkConfig config;
    private String queueProvider;

    public ConfigBean() {
        flowStoreServiceConnector = new FlowStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()), SinkConfig.FLOWSTORE_URL.asString());
    }

    public ConfigBean(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
    }

    public synchronized DpfSinkConfig getConfig() {
        return config;
    }

    public synchronized String getQueueProvider() {
        return queueProvider;
    }

    public void refresh(ConsumedMessage consumedMessage) {
        refreshSinkConfig(consumedMessage);
        refreshQueueProvider(consumedMessage);
    }

    /*
     * Refreshes the sink config contained in this bean by flow-store lookup if it is outdated
     * @param consumedMessage consumed message containing the version and the id of the sink
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void refreshSinkConfig(ConsumedMessage consumedMessage) {
        try {
            long sinkId = JMSHeader.sinkId.getHeader(consumedMessage, Long.class);
            long sinkVersion = JMSHeader.sinkVersion.getHeader(consumedMessage, Long.class);
            synchronized (this) {
                if (sinkVersion > highestVersionSeen) {
                    Sink sink = flowStoreServiceConnector.getSink(sinkId);
                    config = (DpfSinkConfig) sink.getContent().getSinkConfig();
                    LOGGER.info("Current sink config: {}", config);
                    highestVersionSeen = sink.getVersion();
                }
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void refreshQueueProvider(ConsumedMessage message) {
        try {
            long flowBinderIdFromMessage = JMSHeader.flowBinderId.getHeader(message, Long.class);
            long flowBinderVersionFromMessage = JMSHeader.flowBinderVersion.getHeader(message, Long.class);
            FlowBinder flowBinder = cachedFlowBinders.getIfPresent(flowBinderIdFromMessage);
            synchronized (this) {
                if (flowBinder == null || flowBinder.getVersion() < flowBinderVersionFromMessage) {
                    flowBinder = flowStoreServiceConnector.getFlowBinder(flowBinderIdFromMessage);
                    LOGGER.info("Caching version {} of flow-binder {}", flowBinder.getVersion(), flowBinder.getContent().getName());
                    cachedFlowBinders.put(flowBinderIdFromMessage, flowBinder);
                    queueProvider = flowBinder.getContent().getQueueProvider();
                }
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
