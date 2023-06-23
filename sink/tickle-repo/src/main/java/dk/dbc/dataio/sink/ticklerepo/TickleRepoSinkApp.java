package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class TickleRepoSinkApp extends MessageConsumerApp {
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, tickleRepo);

    public static void main(String[] args) {
        new TickleRepoSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
