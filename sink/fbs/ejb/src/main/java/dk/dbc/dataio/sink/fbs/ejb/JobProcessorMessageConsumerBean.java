package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.util.types.SinkException;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

@MessageDriven
public class JobProcessorMessageConsumerBean extends AbstractSinkMessageConsumerBean {
    @EJB
    FbsPusherBean fbsPusher;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException {
        final SinkChunkResult sinkChunkResult = fbsPusher.push(unmarshallPayload(consumedMessage));
        jobProcessorMessageProducer.send(sinkChunkResult);
    }
}
