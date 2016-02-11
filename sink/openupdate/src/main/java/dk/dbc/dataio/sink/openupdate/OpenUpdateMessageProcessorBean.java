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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
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

import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;

@MessageDriven
public class OpenUpdateMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateMessageProcessorBean.class);

    private final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller = new UpdateRecordResultMarshaller();

    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB OpenUpdateConfigBean openUpdateConfigBean;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException, NullPointerException {
        final Chunk processedChunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Chunk received successfully. Chunk ID: " + processedChunk.getChunkId() + ", Job ID: " + processedChunk.getJobId());
        final Chunk chunkForDelivery = buildBasicChunkForDeliveryFromProcessedChunk(processedChunk);
        try {
            for (ChunkItem processedChunkItem : processedChunk) {
                DBCTrackedLogContext.setTrackingId("traceid:" + processedChunkItem.getTrackingId());
                final OpenUpdateServiceConnector openUpdateServiceConnector = openUpdateConfigBean.getConnector(consumedMessage);
                final AddiRecordsToItemWrapper addiRecordsToItemWrapper = new AddiRecordsToItemWrapper(
                        processedChunkItem, addiRecordPreprocessor, openUpdateServiceConnector, updateRecordResultMarshaller);

                switch (processedChunkItem.getStatus()) {
                    case SUCCESS: chunkForDelivery.insertItem(addiRecordsToItemWrapper.callOpenUpdateWebServiceForEachAddiRecord());
                        break;

                    case FAILURE: chunkForDelivery.insertItem(ObjectFactory.buildIgnoredChunkItem(
                            processedChunkItem.getId(), "Failed by processor", processedChunkItem.getTrackingId()));
                        break;

                    case IGNORE: chunkForDelivery.insertItem(ObjectFactory.buildIgnoredChunkItem(
                            processedChunkItem.getId(), "Ignored by processor", processedChunkItem.getTrackingId()));
                        break;

                    default: throw new SinkException("Unknown chunk item state: " + processedChunkItem.getStatus().name());
                }
                LOGGER.info("Handled item {} in chunk {} in job {}", processedChunkItem.getId(), processedChunk.getChunkId(), processedChunk.getJobId());
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        addChunkInJobStore(chunkForDelivery);
    }

    private void addChunkInJobStore(Chunk chunkForDelivery) throws SinkException {
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(chunkForDelivery, chunkForDelivery.getJobId(), chunkForDelivery.getChunkId());
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

    private Chunk buildBasicChunkForDeliveryFromProcessedChunk(Chunk processedChunk) {
        final Chunk incompleteDeliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        incompleteDeliveredChunk.setEncoding(processedChunk.getEncoding());
        return incompleteDeliveredChunk;
    }
}