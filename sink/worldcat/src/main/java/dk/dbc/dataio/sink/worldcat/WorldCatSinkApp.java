package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

public class WorldCatSinkApp extends MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCatEntity.class);
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final Supplier<WorldcatMessageConsumer> messageConsumer = () -> new WorldcatMessageConsumer(serviceHub,
            JPAHelper.makeEntityManager("ocnRepoPU", SinkConfig.OCN_REPO_DB_URL));

    public WorldCatSinkApp() {
        LOGGER.info("Migrating db");
        JPAHelper.migrate(SinkConfig.OCN_REPO_DB_URL);
        LOGGER.info("Successfully migrated db");
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
