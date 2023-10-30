package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class TickleRepoSinkApp extends MessageConsumerApp {
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final Supplier<TickleMessageConsumer> MESSAGE_CONSUMER = () -> new TickleMessageConsumer(SERVICE_HUB,
            JPAHelper.makeEntityManagerFactory("tickleRepoPU", SinkConfig.TICKLE_REPO_DB_URL));

    public TickleRepoSinkApp() {
        JPAHelper.migrate(SinkConfig.TICKLE_REPO_DB_URL);
    }

    public static void main(String[] args) {
        new TickleRepoSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
