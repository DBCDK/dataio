package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
public class DiffSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumerBean> messageConsumer = () -> new MessageConsumerBean(serviceHub);

    public static void main(String[] args) {
        new DiffSinkApp().go(serviceHub, messageConsumer);
    }
}
