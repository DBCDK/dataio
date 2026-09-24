package dk.dbc.dataio.sink.dpf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.commons.useragent.UserAgent;
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
    /** Read outside the monitor to decide whether a lookup is needed, and written under it. */
    private volatile long highestVersionSeen = 0;
    private DpfSinkConfig config;
    private String queueProvider;

    @SuppressWarnings("java:S2095")
    public ConfigBean() {
        flowStoreServiceConnector = new FlowStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()),
                UserAgent.forInternalRequests(), SinkConfig.FLOWSTORE_URL.asString());
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

    /**
     * Brings the sink config and the queue provider up to the versions the given message names
     * <p>
     * The queue provider is returned as well as stored, so that one delivery uses one value. The
     * consumer threads sharing this bean all write the field, and a delivery reading it back
     * separately can pick up the value another thread's flow binder supplied.
     *
     * @param consumedMessage message naming the sink and flow binder versions to refresh to
     * @return queue provider of the flow binder the message names
     */
    public String refresh(ConsumedMessage consumedMessage) {
        refreshSinkConfig(consumedMessage);
        return refreshQueueProvider(consumedMessage);
    }

    /*
     * Refreshes the sink config contained in this bean by flow-store lookup if it is outdated
     *
     * The lookup runs outside the monitor, so that one thread fetching a new sink version does not
     * hold every other consumer thread's config read behind one HTTP request. Two threads can
     * therefore fetch at once, and the condition is tested a second time inside the monitor to keep
     * the older of two concurrently fetched versions from overwriting the newer.
     *
     * @param consumedMessage consumed message containing the version and the id of the sink
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void refreshSinkConfig(ConsumedMessage consumedMessage) {
        try {
            long sinkId = JMSHeader.sinkId.getHeader(consumedMessage, Long.class);
            long sinkVersion = JMSHeader.sinkVersion.getHeader(consumedMessage, Long.class);
            if (sinkVersion > highestVersionSeen) {
                Sink sink = flowStoreServiceConnector.getSink(sinkId);
                synchronized (this) {
                    if (sinkVersion > highestVersionSeen) {
                        config = (DpfSinkConfig) sink.getContent().getSinkConfig();
                        LOGGER.info("Current sink config: {}", config);
                        highestVersionSeen = sink.getVersion();
                    }
                }
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /*
     * The queue provider is taken off the flow binder the message names on every call, cached or
     * freshly fetched, rather than only where the fetch happened. Reading it only on a fetch leaves
     * the field holding another flow binder's value for every message whose own flow binder was
     * already cached at the version it names.
     */
    private String refreshQueueProvider(ConsumedMessage message) {
        try {
            long flowBinderIdFromMessage = JMSHeader.flowBinderId.getHeader(message, Long.class);
            long flowBinderVersionFromMessage = JMSHeader.flowBinderVersion.getHeader(message, Long.class);
            FlowBinder flowBinder = cachedFlowBinders.getIfPresent(flowBinderIdFromMessage);
            if (flowBinder == null || flowBinder.getVersion() < flowBinderVersionFromMessage) {
                flowBinder = flowStoreServiceConnector.getFlowBinder(flowBinderIdFromMessage);
                LOGGER.info("Caching version {} of flow-binder {}", flowBinder.getVersion(), flowBinder.getContent().getName());
                cachedFlowBinders.put(flowBinderIdFromMessage, flowBinder);
            }
            String resolved = flowBinder.getContent().getQueueProvider();
            synchronized (this) {
                queueProvider = resolved;
            }
            return resolved;
        } catch (FlowStoreServiceConnectorException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
