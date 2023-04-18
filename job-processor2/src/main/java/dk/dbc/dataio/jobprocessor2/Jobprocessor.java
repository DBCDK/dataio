package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jobprocessor2.jms.JobStoreMessageConsumer;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

public class Jobprocessor extends MessageConsumerApp {
    private static ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final JobStoreMessageConsumer messageConsumer = new JobStoreMessageConsumer(serviceHub);

    public static void main(String[] args) {
        new Jobprocessor().go(serviceHub, messageConsumer);
    }
}
