/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.vip;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @EJB ConfigBean configBean;

    private VipSinkConfig config;

    @Override
    @Stopwatch
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws InvalidMessageException, SinkException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final VipSinkConfig latestConfig = configBean.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            // // TODO: 11-04-19 Update connector to latest endpoint
            config = latestConfig;
        }

        // TODO: 11-04-19 Message processing
    }
}
