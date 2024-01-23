package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.commons.jsonb.JSONBContext;
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;


public abstract class AbstractFlowStoreServiceContainerTest implements PostgresContainerJPAUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFlowStoreServiceContainerTest.class);
    static final GenericContainer<?> flowstoreServiceContainer;
    static final String flowStoreServiceBaseUrl;
    static final FlowStoreServiceConnector flowStoreServiceConnector;
    static final Connection flowStoreDbConnection;
    protected static final JSONBContext jsonbContext = new JSONBContext();

    static {
        flowstoreServiceContainer = Containers.FLOW_STORE.makeContainer()
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "4G")
                .withEnv("FLOWSTORE_DB_URL", dbContainer.getPayaraDockerJdbcUrl())
                .withEnv("SUBVERSION_URL", "https://no-svn-server-needed-for-this-test")
//                .withEnv("REMOTE_DEBUGGING_HOST", getDebuggingHost())
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

    private static String getDebuggingHost() {
        try {
            String host = InetAddress.getLocalHost().getHostAddress() + ":5005";
            return host;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void initializeDB() {
        URL resource = HarvesterConfigsIT.class.getResource("/initial_state.sql");
        try {
            JDBCUtil.executeScript(flowStoreDbConnection,
                    new File(resource.toURI()), StandardCharsets.UTF_8.name());
        } catch (IOException | URISyntaxException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    static Connection connectToFlowStoreDB() throws SQLException {
        return dbContainer.datasource().getConnection();
    }

}
