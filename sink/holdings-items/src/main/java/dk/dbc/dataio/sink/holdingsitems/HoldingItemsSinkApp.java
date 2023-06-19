package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class HoldingItemsSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> messageConsumer = () -> new MessageConsumer(serviceHub);

    public static void main(String[] args) {
        new HoldingItemsSinkApp().go(serviceHub, messageConsumer);
    }
}
