/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.eclipse.microprofile.metrics.annotation.Timed;

@MessageDriven
public class OpenUpdateMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateMessageProcessorBean.class);

    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB OpenUpdateConfigBean openUpdateConfigBean;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    static final Metadata chunkItemsMetadata = Metadata.builder()
            .withName("handleConsumedMessage-chunkitems-metered")
            .withDisplayName("dataio-sink-openupdate-handleConsumedMessage-chunkitems-metered")
            .withDescription("Number of chunkitems processed")
            .withType(MetricType.METERED)
            .withUnit("chunkitems").build();

    AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();
    Cache<Long, FlowBinder> cachedFlowBinders = CacheManager.createLRUCache(10);

    OpenUpdateSinkConfig config;
    OpenUpdateServiceConnector connector;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException, NullPointerException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final String queueProvider = getQueueProvider(consumedMessage);
        LOGGER.debug("Using queue-provider {}", queueProvider);

        final OpenUpdateSinkConfig latestConfig = openUpdateConfigBean.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating connector");
            connector = getOpenUpdateServiceConnector(latestConfig);
            config = latestConfig;
        }

        final Chunk outcome = buildOutcomeFromProcessedChunk(chunk);
        try {
            for (ChunkItem chunkItem : chunk) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                LOGGER.info("Handling item {}/{}/{}", chunk.getJobId(), chunk.getChunkId(), chunkItem.getId());
                final ChunkItemProcessor chunkItemProcessor = new ChunkItemProcessor(chunkItem,
                        addiRecordPreprocessor, connector, updateRecordResultMarshaller, metricRegistry);

                switch (chunkItem.getStatus()) {
                    case SUCCESS: outcome.insertItem(chunkItemProcessor.processForQueueProvider(queueProvider));
                        break;
                    case FAILURE: outcome.insertItem(
                            ChunkItem.ignoredChunkItem()
                                    .withId(chunkItem.getId())
                                    .withTrackingId(chunkItem.getTrackingId())
                                    .withData("Failed by processor")
                                    .withType(ChunkItem.Type.STRING)
                                    .withEncoding(StandardCharsets.UTF_8));
                        break;
                    case IGNORE: outcome.insertItem(
                            ChunkItem.ignoredChunkItem()
                                    .withId(chunkItem.getId())
                                    .withTrackingId(chunkItem.getTrackingId())
                                    .withData("Ignored by processor")
                                    .withType(ChunkItem.Type.STRING)
                                    .withEncoding(StandardCharsets.UTF_8));
                        break;
                    default: throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        addOutcomeToJobStore(outcome);

        metricRegistry.meter(chunkItemsMetadata, new Tag("queueProvider", queueProvider)).mark(chunk.size());
    }

    private OpenUpdateServiceConnector getOpenUpdateServiceConnector(OpenUpdateSinkConfig config) {
        return new OpenUpdateServiceConnector(
                config.getEndpoint(),
                config.getUserId(),
                config.getPassword());
    }

    private void addOutcomeToJobStore(Chunk outcome) throws SinkException {
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(outcome, outcome.getJobId(), outcome.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            logJobStoreError(e);
            // Throw SinkException to force transaction rollback
            throw new SinkException("Error in communication with job-store", e);
        }
    }

    private void logJobStoreError(JobStoreServiceConnectorException e) {
        if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
            if (jobError != null) {
                LOGGER.error("job-store returned error: {}", jobError.getDescription());
            }
        }
    }

    private Chunk buildOutcomeFromProcessedChunk(Chunk processedChunk) {
        final Chunk outcome = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        outcome.setEncoding(processedChunk.getEncoding());
        return outcome;
    }

    private String getQueueProvider(ConsumedMessage message) throws SinkException {
        try {
            final long flowBinderIdFromMessage = message.getHeaderValue(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, Long.class);
            final long flowBinderVersionFromMessage = message.getHeaderValue(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, Long.class);
            FlowBinder flowBinder = cachedFlowBinders.get(flowBinderIdFromMessage);
            if (flowBinder == null || flowBinder.getVersion() < flowBinderVersionFromMessage) {
                flowBinder = flowStoreServiceConnectorBean.getConnector().getFlowBinder(flowBinderIdFromMessage);
                LOGGER.info("Caching version {} of flow-binder {}",
                        flowBinder.getVersion(), flowBinder.getContent().getName());
                cachedFlowBinders.put(flowBinderIdFromMessage, flowBinder);
            }
            return flowBinder.getContent().getQueueProvider();
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}