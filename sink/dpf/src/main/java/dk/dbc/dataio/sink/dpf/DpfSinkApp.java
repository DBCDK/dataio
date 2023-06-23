package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class DpfSinkApp extends MessageConsumerApp {
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final ServiceBroker SERVICE_BROKER = new ServiceBroker();
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, SERVICE_BROKER);

    public static void main(String[] args) {
        new DpfSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
