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

package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;

@MessageDriven
public class DummyMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyMessageProcessorBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final Chunk processedChunk = unmarshallPayload(consumedMessage);
        final Chunk deliveredChunk = processPayload(processedChunk);
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new EJBException(e);
        }
    }

    Chunk processPayload(Chunk processedChunk) {
        final Chunk deliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (final ChunkItem item : processedChunk) {
                final String trackingId = item.getTrackingId();
                DBCTrackedLogContext.setTrackingId("traceid:" + trackingId);
                // Set new-item-status to success if chunkResult-item was success - else set new-item-status to ignore:
                ChunkItem.Status status = item.getStatus() == ChunkItem.Status.SUCCESS ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE;
                ChunkItem chunkItem = new ChunkItem(item.getId(), StringUtil.asBytes("Set by DummySink"), status);
                chunkItem.setTrackingId(trackingId);
                deliveredChunk.insertItem(chunkItem);
                LOGGER.debug("Handled chunk item {} for chunk {} in job {}",
                        chunkItem.getId(), processedChunk.getChunkId(),processedChunk.getJobId());
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return deliveredChunk;
    }
}
