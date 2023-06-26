package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.function.Supplier;

public class TickleRepoSinkApp extends MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleRepoSinkApp.class);
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, makeEntityManager());

    private static EntityManager makeEntityManager() {
        try {
            LOGGER.info("Creating entity manager");
            Map<String, Object> config = Map.of(
                    "javax.persistence.transactionType", "RESOURCE_LOCAL",
                    "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                    "javax.persistence.schema-generation.database.action", "none",
                    "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                    "javax.persistence.jdbc.url", SinkConfig.TICKLE_REPO_DB_URL.asPGJDBCUrl());
            EntityManager entityManager = Persistence.createEntityManagerFactory("tickleRepoPU", config).createEntityManager();
            LOGGER.info("Created entity manager");
            return entityManager;
        } catch (RuntimeException re) {
            LOGGER.error("Failed to create entity manager", re);
            throw re;
        }
    }

    public static void main(String[] args) {
        new TickleRepoSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
