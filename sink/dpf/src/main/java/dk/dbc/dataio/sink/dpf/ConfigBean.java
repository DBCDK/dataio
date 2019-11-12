/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the DPF sink
 */
@Singleton
public class ConfigBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigBean.class);

    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    Cache<Long, FlowBinder> cachedFlowBinders = CacheManager.createLRUCache(10);

    private long highestVersionSeen = 0;
    private DpfSinkConfig config;
    private String queueProvider;

    @Lock(LockType.READ)
    public DpfSinkConfig getConfig() {
        return config;
    }

    @Lock(LockType.READ)
    public String getQueueProvider() {
        return queueProvider;
    }

    public void refresh(ConsumedMessage consumedMessage) throws SinkException {
        refreshSinkConfig(consumedMessage);
        refreshQueueProvider(consumedMessage);
    }

    /*
     * Refreshes the sink config contained in this bean by flow-store lookup if it is outdated
     * @param consumedMessage consumed message containing the version and the id of the sink
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void refreshSinkConfig(ConsumedMessage consumedMessage) throws SinkException {
        try {
            final long sinkId = consumedMessage.getHeaderValue(JmsConstants.SINK_ID_PROPERTY_NAME, Long.class);
            final long sinkVersion = consumedMessage.getHeaderValue(JmsConstants.SINK_VERSION_PROPERTY_NAME, Long.class);
            if (sinkVersion > highestVersionSeen) {
                final Sink sink = flowStoreServiceConnectorBean.getConnector().getSink(sinkId);
                config = (DpfSinkConfig) sink.getContent().getSinkConfig();
                LOGGER.info("Current sink config: {}", config);
                highestVersionSeen = sink.getVersion();
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }

    private void refreshQueueProvider(ConsumedMessage message) throws SinkException {
        try {
            final long flowBinderIdFromMessage = message.getHeaderValue(
                    JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, Long.class);
            final long flowBinderVersionFromMessage = message.getHeaderValue(
                    JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, Long.class);
            FlowBinder flowBinder = cachedFlowBinders.get(flowBinderIdFromMessage);
            if (flowBinder == null || flowBinder.getVersion() < flowBinderVersionFromMessage) {
                flowBinder = flowStoreServiceConnectorBean.getConnector().getFlowBinder(flowBinderIdFromMessage);
                LOGGER.info("Caching version {} of flow-binder {}",
                        flowBinder.getVersion(), flowBinder.getContent().getName());
                cachedFlowBinders.put(flowBinderIdFromMessage, flowBinder);
                queueProvider = flowBinder.getContent().getQueueProvider();
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}
