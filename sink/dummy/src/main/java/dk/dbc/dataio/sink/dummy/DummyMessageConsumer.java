package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    public DummyMessageConsumer(ServiceHub serviceHub) {
        super(serviceHub);
    }

    /**
     * Reports the item without sending it anywhere, since this sink has no target system
     * <p>
     * A successfully processed item is reported as delivered, anything else as ignored.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        boolean processedOk = item.getStatus() == ChunkItem.Status.SUCCESS;
        ChunkItem outcome = new ChunkItem()
                .withId(item.getId())
                .withStatus(processedOk ? ChunkItem.Status.SUCCESS : ChunkItem.Status.IGNORE)
                .withTrackingId(item.getTrackingId())
                .withData("Set by DummySink")
                .withType(ChunkItem.Type.STRING);
        LOGGER.debug("Handled item {}", item.getId());
        return ItemDeliveryResult.of(
                processedOk ? ItemDeliveryResult.Status.DELIVERED : ItemDeliveryResult.Status.IGNORED,
                outcome);
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    @Override
    public String getFilter() {
        return SinkConfig.MESSAGE_FILTER.asOptionalString().orElse(null);
    }
}
