package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;

import java.util.function.Supplier;

public class BatchExchangeSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final EntityManager ENTITY_MANAGER = JPAHelper.makeEntityManager("batchExchangePU", SinkConfig.BATCH_EXCHANGE_DB_URL);
    private static final Supplier<BatchExchangeMessageConsumer> messageConsumer = BatchExchangeSinkApp::makeConsumer;

    public BatchExchangeSinkApp() {
        new ScheduledBatchFinalizer(serviceHub, ENTITY_MANAGER);
        JPAHelper.migrate(SinkConfig.BATCH_EXCHANGE_DB_URL);
    }

    public static void main(String[] args) {
        new BatchExchangeSinkApp().go(serviceHub, messageConsumer);
    }

    private static BatchExchangeMessageConsumer makeConsumer() {
        return new BatchExchangeMessageConsumer(serviceHub, JPAHelper.makeEntityManager("batchExchangePU", SinkConfig.BATCH_EXCHANGE_DB_URL));
    }
}
