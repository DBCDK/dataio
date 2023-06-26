package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.eclipse.persistence.internal.jpa.deployment.SEPersistenceUnitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TickleRepoSinkApp extends MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleRepoSinkApp.class);
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, makeEntityManager());

    private static EntityManager makeEntityManager() {
        LOGGER.info("Creating entiymanager");
        SEPersistenceUnitInfo puInfo = new SEPersistenceUnitInfo();
        try {
            puInfo.setPersistenceUnitRootUrl(new URL("http://dataio/jse"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        puInfo.setPersistenceUnitName("tickleRepoPU1");
        puInfo.setManagedClassNames(List.of(
                "dk.dbc.jsonb.JsonConverter",
                "dk.dbc.ticklerepo.dto.DataSet",
                "dk.dbc.ticklerepo.dto.BatchTypeConverter",
                "dk.dbc.ticklerepo.dto.Batch",
                "dk.dbc.ticklerepo.dto.RecordStatusConverter",
                "dk.dbc.ticklerepo.dto.Record"));
        Map<String, Object> config = Map.of(
                "javax.persistence.transactionType", "RESOURCE_LOCAL",
                "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                "javax.persistence.schema-generation.database.action", "none",
                "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                "javax.persistence.jdbc.url", SinkConfig.TICKLE_REPO_DB_URL.asPGJDBCUrl(),
                "eclipselink.se-puinfo", puInfo);
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("tickleRepoPU1", config);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        LOGGER.info("Created entiymanager");
        return entityManager;
    }

    public static void main(String[] args) {
        new TickleRepoSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
