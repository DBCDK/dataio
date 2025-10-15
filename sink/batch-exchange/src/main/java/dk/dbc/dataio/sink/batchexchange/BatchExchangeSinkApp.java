package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManagerFactory;

import java.util.function.Supplier;

public class BatchExchangeSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final EntityManagerFactory ENTITY_MANAGER = JPAHelper.makeEntityManagerFactory("batchExchangePU", SinkConfig.BATCH_EXCHANGE_DB_URL);
    private static final Supplier<BatchExchangeMessageConsumer> messageConsumer = BatchExchangeSinkApp::makeConsumer;

    public BatchExchangeSinkApp() {
        new ScheduledBatchFinalizer(serviceHub, ENTITY_MANAGER);
        JPAHelper.migrate(SinkConfig.BATCH_EXCHANGE_DB_URL, c -> c.locations("classpath:dk/dbc/batchexchange/db/migration"), c -> c.baselineOnMigrate(true));
    }

    public static void main(String[] args) {
        new BatchExchangeSinkApp().go(serviceHub, messageConsumer);
    }

    private static BatchExchangeMessageConsumer makeConsumer() {
        return new BatchExchangeMessageConsumer(serviceHub, JPAHelper.makeEntityManagerFactory("batchExchangePU", SinkConfig.BATCH_EXCHANGE_DB_URL));
    }
}
