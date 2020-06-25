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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.nio.charset.StandardCharsets;

/**
 * This message driven bean monitors the DMQ for dead chunks
 * ensuring that they are marked as completed with failures in
 * the underlying store
 */
@MessageDriven
public class DmqMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmqMessageConsumerBean.class);

    @EJB PgJobStore jobStoreBean;
    @EJB JobSchedulerBean jobSchedulerBean;

    JSONBContext jsonbContext = new JSONBContext();

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, JobStoreException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received dead message for chunk {} of type {} in job {}",
                    chunk.getChunkId(), chunk.getType(), chunk.getJobId());
            if (chunk.getType() == Chunk.Type.PARTITIONED) {
                final Chunk deadChunk = createDeadChunk(Chunk.Type.PROCESSED, chunk);
                jobSchedulerBean.chunkProcessingDone(deadChunk);
                jobStoreBean.addChunk(deadChunk);
            } else {
                final Chunk deadChunk = createDeadChunk(Chunk.Type.DELIVERED, chunk);
                jobSchedulerBean.chunkDeliveringDone(deadChunk);
                jobStoreBean.addChunk(deadChunk);
            }
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid %s type",
                    consumedMessage.getMessageId(), consumedMessage.getMessagePayload()), e);
        }
    }

    private Chunk createDeadChunk(Chunk.Type chunkType, Chunk originatingChunk) {
        final Chunk deadChunk = new Chunk(originatingChunk.getJobId(), originatingChunk.getChunkId(), chunkType);
        deadChunk.setEncoding(StandardCharsets.UTF_8);
        for (ChunkItem chunkItem : originatingChunk) {
            deadChunk.insertItem(new ChunkItem()
                    .withId(chunkItem.getId())
                    .withData(StringUtil.asBytes(String.format(
                            "Item was failed due to dead %s chunk", originatingChunk.getType())))
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withType(originatingChunk.isTerminationChunk() ?
                            ChunkItem.Type.JOB_END : ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId()));
        }
        return deadChunk;
    }
}
