package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class PeriodicJobsSinkApp extends MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsSinkApp.class);
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<PeriodicJobsMessageConsumer> messageConsumer = () -> new PeriodicJobsMessageConsumer(serviceHub,
            JPAHelper.makeEntityManagerFactory("periodic-jobs_PU", SinkConfig.PERIODIC_JOBS_DB_URL));
    public PeriodicJobsSinkApp() {
        LOGGER.info("Migrating db");
        JPAHelper.migrate(SinkConfig.PERIODIC_JOBS_DB_URL);
        LOGGER.info("Successfully migrated db");
    }
    public static void main(String[] args) {
        LOGGER.info("Starting PeriodicJobsSinkApp");
        new PeriodicJobsSinkApp().go(serviceHub, messageConsumer);
        LOGGER.info("Successfully started PeriodicJobsSinkApp");
    }
}
