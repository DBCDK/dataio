package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.ticklerepo.TickleRepo;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.function.Supplier;

public class TickleRepoSinkApp extends MessageConsumerApp {
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, makeTickleRepo());

    private static TickleRepo makeTickleRepo() {
        Map<String, String> config = Map.of(
                "javax.persistence.transactionType", "RESOURCE_LOCAL",
                "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                "javax.persistence.schema-generation.database.action", "none",
                "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                "javax.persistence.jdbc.url", SinkConfig.TICKLE_REPO_DB_URL.asPGJDBCUrl());
        EntityManager entityManager = Persistence.createEntityManagerFactory("tickleRepoPU", config).createEntityManager();
        return new TickleRepo(entityManager);
    }

    public static void main(String[] args) {
        new TickleRepoSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
