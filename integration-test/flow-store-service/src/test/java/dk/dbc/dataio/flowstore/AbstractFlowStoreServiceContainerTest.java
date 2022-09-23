package dk.dbc.dataio.flowstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

public abstract class AbstractFlowStoreServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowStoreServiceContainerTest.class);

    static {
        Testcontainers.exposeHostPorts(Integer.parseInt(
                System.getProperty("flowstore.it.postgresql.port")));
    }

    static final GenericContainer<?> flowstoreServiceContainer;
    static final String flowStoreServiceBaseUrl;
    static final FlowStoreServiceConnector flowStoreServiceConnector;
    static final Connection flowStoreDbConnection;

    static {
        flowstoreServiceContainer = Containers.FLOW_STORE.makeContainer()
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "4G")
                .withEnv("FLOWSTORE_DB_URL", String.format("%s:%s@host.testcontainers.internal:%s/%s",
                        System.getProperty("user.name"),
                        System.getProperty("user.name"),
                        System.getProperty("flowstore.it.postgresql.port"),
                        System.getProperty("flowstore.it.postgresql.dbname")))
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp(System.getProperty("flowstore.it.service.context") + "/status"))
                .withStartupTimeout(Duration.ofMinutes(5));
        flowstoreServiceContainer.start();
        flowStoreDbConnection = connectToFlowStoreDB();
        flowStoreServiceBaseUrl = "http://" + flowstoreServiceContainer.getContainerIpAddress() +
                ":" + flowstoreServiceContainer.getMappedPort(8080) +
                System.getProperty("flowstore.it.service.context");
        flowStoreServiceConnector = new FlowStoreServiceConnector(
                FailSafeHttpClient.create(HttpClient.newClient(), new RetryPolicy().withMaxRetries(0)),
                flowStoreServiceBaseUrl);
    }

    static Connection connectToFlowStoreDB() {
        try {
            Class.forName("org.postgresql.Driver");
            final String dbUrl = String.format("jdbc:postgresql://localhost:%s/%s",
                    System.getProperty("flowstore.it.postgresql.port"),
                    System.getProperty("flowstore.it.postgresql.dbname"));
            final Connection connection = DriverManager.getConnection(dbUrl,
                    System.getProperty("user.name"),
                    System.getProperty("user.name"));
            connection.setAutoCommit(true);
            return connection;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
