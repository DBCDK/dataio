package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class BatchExchangeSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> messageConsumer = BatchExchangeSinkApp::makeConsumer;

    public BatchExchangeSinkApp() {
        JPAHelper.migrate(SinkConfig.BATCH_EXCHANGE_DB_URL);
    }

    public static void main(String[] args) {
        new BatchExchangeSinkApp().go(serviceHub, messageConsumer);
    }

    private static MessageConsumer makeConsumer() {
        return new MessageConsumer(serviceHub, JPAHelper.makeEntityManager("batchExchangePU", SinkConfig.BATCH_EXCHANGE_DB_URL));
    }
}
