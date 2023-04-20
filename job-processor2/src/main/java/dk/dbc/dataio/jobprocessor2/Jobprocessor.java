package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jobprocessor2.jms.JobStoreMessageConsumer;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class Jobprocessor extends MessageConsumerApp {
    private static ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<JobStoreMessageConsumer> messageConsumer = () -> new JobStoreMessageConsumer(serviceHub);

    public static void main(String[] args) {
        new Jobprocessor().go(serviceHub, messageConsumer);
    }
}
