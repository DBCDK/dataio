package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.ocnrepo.OcnRepoDatabaseMigrator;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.util.Map;
import java.util.function.Supplier;

public class WorldCatSinkApp extends MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCatEntity.class);
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<MessageConsumer> messageConsumer = () -> new MessageConsumer(serviceHub, makeEntityManager());

    public WorldCatSinkApp() {
        LOGGER.info("Migrating db");
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(SinkConfig.OCN_REPO_DB_URL.asPGJDBCUrl());
        new OcnRepoDatabaseMigrator(dataSource).migrate();
        LOGGER.info("Succesfully migrated db");
    }

        public static EntityManager makeEntityManager() {
        Map<String, String> config = Map.of(
                "javax.persistence.transactionType", "RESOURCE_LOCAL",
                "provider", "org.eclipse.persistence.jpa.PersistenceProvider",
                "javax.persistence.schema-generation.database.action", "none",
                "javax.persistence.jdbc.driver", "org.postgresql.Driver",
                "javax.persistence.jdbc.url", SinkConfig.OCN_REPO_DB_URL.asPGJDBCUrl());
        return Persistence.createEntityManagerFactory("ocnRepoPU", config).createEntityManager();
    }
    private static void handleProxySettings(Boolean proxied) {
        if (proxied) {
            Map.of("dk.dbc.enableCustomProxy","true",
                    "https.proxyPort","3128",
                    "https.proxyHost","dmzproxy.dbc.dk",
                    "https.nonProxyHosts","localhost|*.dbc.dk|*.addi.dk",
                    "http.proxyPort","3128",
                    "http.proxyHost","dmzproxy.dbc.dk",
                    "http.nonProxyHosts","localhost|*.dbc.dk|*.addi.dk")
                    .forEach(System::setProperty);
            LOGGER.info("Proxied using using: {}", System.getProperty("http.proxyHost"));
        } else {
            LOGGER.info("Not proxied.");
        }
    }
    public static void main(String[] args) {
        LOGGER.info("Starting worlcat sink");
        handleProxySettings(SinkConfig.USE_PROXY.asBoolean());
        new WorldCatSinkApp().go(serviceHub, messageConsumer);
        LOGGER.info("Succesfully started worlcat sink");
    }
}
