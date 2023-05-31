package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jobprocessor2.jms.JobStoreMessageConsumer;
import dk.dbc.dataio.jobprocessor2.jms.NoOpMessageConsumer;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import java.util.function.Supplier;

public class NoOpJobprocessor extends Jobprocessor {
    private static ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<JobStoreMessageConsumer> messageConsumer = () -> new NoOpMessageConsumer(serviceHub);

    public static void main(String[] args) {
        new NoOpJobprocessor().go(serviceHub, messageConsumer);
    }
}
