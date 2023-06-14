package dk.dbc.dataio.sink.marcconv.rest;

import dk.dbc.dataio.jse.artemis.common.DBProperty;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.MessageConsumer;
import dk.dbc.dataio.sink.marcconv.SinkConfig;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.function.Supplier;

public class MarcConvSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final EntityManager entityManager = makeEntityManager();
    private static final Supplier<MessageConsumer> messageConsumer = () -> new MessageConsumer(serviceHub, entityManager);

    private static EntityManager makeEntityManager() {
        Map<DBProperty, String> dbProperties = SinkConfig.MARCCONV_DB_URL.asDBProperties();
        Map<String, String> config = Map.of(
                "javax.persistence.transactionType", "RESOURCE_LOCAL",
                "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                "javax.persistence.schema-generation.database.action", "none",
                "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                "javax.persistence.jdbc.url", SinkConfig.MARCCONV_DB_URL.asPGJDBCUrl(),
                "javax.persistence.jdbc.user", dbProperties.get(DBProperty.USER),
                "javax.persistence.jdbc.password", dbProperties.get(DBProperty.PASSWORD));
        return Persistence.createEntityManagerFactory("marcconv_PU", config).createEntityManager();
    }

    public static void main(String[] args) {
        new MarcConvSinkApp().go(serviceHub, messageConsumer);
    }
}
