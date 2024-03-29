package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class DummySinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<DummyMessageConsumer> messageConsumer = () -> new DummyMessageConsumer(serviceHub);

    public static void main(String[] args) {
        new DummySinkApp().go(serviceHub, messageConsumer);
    }
}
