package dk.dbc.dataio.dlq.errorhandler;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class DLQErrorHandler extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<DLQMessageConsumer> messageConsumer = () -> new DLQMessageConsumer(serviceHub);

    public static void main(String[] args) {
        new DLQErrorHandler().go(serviceHub, messageConsumer);
    }
}
