package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class DummyMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final ChunkResult chunkResult = unmarshallPayload(consumedMessage);
        final SinkChunkResult sinkChunkResult = processPayload(chunkResult);
        jobProcessorMessageProducer.send(sinkChunkResult);
    }

    SinkChunkResult processPayload(ChunkResult chunkResult) {
        final List<ChunkItem> sinkItems = new ArrayList<>(chunkResult.getItems().size());
        for (final ChunkItem item : chunkResult.getItems()) {
            // Set new-item-status to success if chunkResult-item was success - else set new-item-status to ignore:
            ChunkItem.Status status = item.getStatus() == ChunkItem.Status.SUCCESS ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE;
            sinkItems.add(new ChunkItem(item.getId(), "Set by DummySink", status));
        }
        return new SinkChunkResult(chunkResult.getJobId(), chunkResult.getChunkId(), chunkResult.getEncoding(), sinkItems);
    }
}
