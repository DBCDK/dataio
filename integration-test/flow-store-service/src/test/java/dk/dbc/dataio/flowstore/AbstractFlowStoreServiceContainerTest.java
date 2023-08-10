package dk.dbc.dataio.flowstore;

import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

public abstract class AbstractFlowStoreServiceContainerTest implements PostgresContainerJPAUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowStoreServiceContainerTest.class);
    static final GenericContainer<?> flowstoreServiceContainer;
    static final String flowStoreServiceBaseUrl;
    static final FlowStoreServiceConnector flowStoreServiceConnector;
    static final Connection flowStoreDbConnection;

    static {
        flowstoreServiceContainer = Containers.FLOW_STORE.makeContainer()
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "4G")
                .withEnv("FLOWSTORE_DB_URL", dbContainer.getPayaraDockerJdbcUrl())
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp(System.getProperty("flowstore.it.service.context") + "/status"))
                .withStartupTimeout(Duration.ofMinutes(5));
        flowstoreServiceContainer.start();
        try {
            flowStoreDbConnection = connectToFlowStoreDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        flowStoreServiceBaseUrl = "http://" + flowstoreServiceContainer.getContainerIpAddress() +
                ":" + flowstoreServiceContainer.getMappedPort(8080) +
                System.getProperty("flowstore.it.service.context");
        flowStoreServiceConnector = new FlowStoreServiceConnector(
                FailSafeHttpClient.create(HttpClient.newClient(), new RetryPolicy().withMaxRetries(0)),
                flowStoreServiceBaseUrl);
    }

    static Connection connectToFlowStoreDB() throws SQLException {
        return dbContainer.datasource().getConnection();
    }
}
