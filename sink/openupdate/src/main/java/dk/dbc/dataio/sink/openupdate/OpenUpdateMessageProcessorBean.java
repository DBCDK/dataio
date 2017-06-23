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
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

@MessageDriven
public class OpenUpdateMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateMessageProcessorBean.class);

    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB OpenUpdateConfigBean openUpdateConfigBean;

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
                        addiRecordPreprocessor, connector, updateRecordResultMarshaller);

                switch (chunkItem.getStatus()) {
                    case SUCCESS: outcome.insertItem(chunkItemProcessor.processForQueueProvider(queueProvider));
                        break;

                    case FAILURE: outcome.insertItem(ObjectFactory.buildIgnoredChunkItem(
                            chunkItem.getId(), "Failed by processor", chunkItem.getTrackingId()));
                        break;

                    case IGNORE: outcome.insertItem(ObjectFactory.buildIgnoredChunkItem(
                            chunkItem.getId(), "Ignored by processor", chunkItem.getTrackingId()));
                        break;

                    default: throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        addOutcomeToJobStore(outcome);
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