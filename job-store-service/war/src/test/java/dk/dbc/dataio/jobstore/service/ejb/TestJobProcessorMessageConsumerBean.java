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

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Handles Chunk messages received from the job-store
 * Test Job Chunk Processor
 *
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/dataio/processor"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"), }
)
public class TestJobProcessorMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobProcessorMessageConsumerBean.class);

    private static final List<Chunk> chunksReceived =new ArrayList<>();
    private static final Semaphore processBlocker=new Semaphore(0);

    JSONBContext jsonbContext = new JSONBContext();


    @SuppressWarnings("EjbClassWarningsInspection")
    static void waitForProcessingOfChunks(String message, int numberOfChunksToWaitFor) throws Exception {
        StopWatch timer=new StopWatch();
        if( ! processBlocker.tryAcquire( numberOfChunksToWaitFor, 10, TimeUnit.SECONDS ) ) {
            throw new Exception("Unittest Errors unable to Aacquire "+ numberOfChunksToWaitFor + " in 10 Seconds :"+message);
        }
        LOGGER.info("Waiting in took waitForProcessingOfChunks {}  {} ms", numberOfChunksToWaitFor, timer.getElapsedTime());
    }

    /**
     * Processes Chunk received in consumed message
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     */
    @Stopwatch
    synchronized public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            process(chunk);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getHeaderValue(JmsConstants.CHUNK_PAYLOAD_TYPE, String.class)), e);
        }
    }

    private void process(Chunk chunk) {
        synchronized (chunksReceived) {
            chunksReceived.add( chunk);
            processBlocker.release();
        }
    }

    @SuppressWarnings("EjbClassWarningsInspection")
    public static void reset() {
        synchronized (chunksReceived) {
            chunksReceived.clear();
            processBlocker.drainPermits();
        }
    }

    @SuppressWarnings("EjbClassWarningsInspection")
    public static int getChunksReceivedCount() {
        synchronized (( chunksReceived )) {
            return chunksReceived.size();
        }
    }
}
