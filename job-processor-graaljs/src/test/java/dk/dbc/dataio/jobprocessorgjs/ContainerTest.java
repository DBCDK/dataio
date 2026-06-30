package dk.dbc.dataio.jobprocessorgjs;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.commons.testcontainers.service.DBCServiceContainer;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public abstract class ContainerTest {
    public static final String QUEUE_NAME = "processor";

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);

    public static final GenericContainer<?> artemisContainer;
    public static final int ARTEMIS_PORT;
    public static final WireMockServer wireMockServer;
    public static final DBCPostgreSQLContainer logStoreDbContainer;
    public static final DBCServiceContainer serviceContainer;

    static {
        LOGGER.info("Starting Artemis Container");
        artemisContainer = Containers.ARTEMIS.makeContainer()
                .withExposedPorts(61616)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withStartupTimeout(Duration.ofMinutes(1));
        artemisContainer.start();
        LOGGER.info("Started Artemis Container");

        ARTEMIS_PORT = artemisContainer.getMappedPort(61616);

        // The processor declares a jdbc/dataio/logstore connection pool from LOGSTORE_DB_URL,
        // so it needs a reachable log-store database to boot. The schema is irrelevant here —
        // the test flow logs nothing under dk.dbc.js, so no rows are written.
        logStoreDbContainer = new DBCPostgreSQLContainer().withReuse(false);
        logStoreDbContainer.start();
        logStoreDbContainer.exposeHostPort();

        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        Testcontainers.exposeHostPorts(ARTEMIS_PORT, wireMockServer.port());

        serviceContainer = new DBCServiceContainer(readDockerImage())
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "1g")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("LOG__dk_dbc", "DEBUG")
                .withEnv("ARTEMIS_MQ_HOST", "host.testcontainers.internal")
                .withEnv("ARTEMIS_JMS_PORT", String.valueOf(ARTEMIS_PORT))
                .withEnv("ARTEMIS_USER", "admin")
                .withEnv("ARTEMIS_PASSWORD", "GoFish")
                .withEnv("QUEUE", QUEUE_NAME)
                .withEnv("JOBSTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("FLOWSTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("LOGSTORE_DB_URL", logStoreDbContainer.getPayaraDockerJdbcUrl())
                .withHttpClient(createHttpClient())
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/health/ready"))
                .withStartupTimeout(Duration.ofMinutes(2));
        serviceContainer.start();
    }

    private static String readDockerImage() {
        try {
            return Files.readString(Path.of("target/docker.out")).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("target/docker.out not found — run 'mvn package' first", e);
        }
    }

    public static HttpClient createHttpClient() {
        return HttpClient.create(
                ClientBuilder.newBuilder()
                        .register(JacksonFeature.class)
                        .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
                        .build(),
                new UserAgent(ContainerTest.class.getSimpleName()));
    }
}
