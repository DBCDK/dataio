package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

@MessageDriven
public class DummyMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final ExternalChunk deliveredChunk = processPayload(processedChunk);
        jobProcessorMessageProducer.send(deliveredChunk);
    }

    ExternalChunk processPayload(ExternalChunk processedChunk) {
        ExternalChunk deliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        for (final ChunkItem item : processedChunk) {
            // Set new-item-status to success if chunkResult-item was success - else set new-item-status to ignore:
            ChunkItem.Status status = item.getStatus() == ChunkItem.Status.SUCCESS ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE;
            deliveredChunk.insertItem(new ChunkItem(item.getId(), "Set by DummySink", status));
        }
        return deliveredChunk;
    }
}
