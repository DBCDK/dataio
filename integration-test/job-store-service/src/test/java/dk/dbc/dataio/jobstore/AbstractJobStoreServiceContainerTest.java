package dk.dbc.dataio.jobstore;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jms.JmsQueueServiceConnector;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static dk.dbc.dataio.commons.types.JobSpecification.JOB_EXPIRATION_AGE_IN_DAYS;

@SuppressWarnings({"SameParameterValue", "resource"})
public abstract class AbstractJobStoreServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobStoreServiceContainerTest.class);
    private static final Network network = Network.newNetwork();

    private static final String ARTEMIS_ALIAS = "dataio-artemis";
    private static final String LOGSTORE = "dataio-logstore";
    private static final String JMS_QUEUE_SERVICE_ALIAS = "dataio-jms-queue-service";
    private static final String JOBSTORE_SERVICE_ALIAS = "dataio-jobstore-service";
    private static final LocalDateTime oldJobDateTime = LocalDateTime.now().minus(JOB_EXPIRATION_AGE_IN_DAYS + 2, ChronoUnit.DAYS);
    private static final LocalDateTime aLittleYoungerJobDateTime = LocalDateTime.now().minus(JOB_EXPIRATION_AGE_IN_DAYS - 1, ChronoUnit.DAYS);
    private static final LocalDateTime jobFromTheDayBeforeThedayBeforeYesterday = LocalDateTime.now().minus(3, ChronoUnit.DAYS);
    private static final LocalDateTime jobFromToday = LocalDateTime.now().minus(2, ChronoUnit.HOURS);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final WireMockServer wireMockServer = startWireMockServer();
    private static final DBCPostgreSQLContainer logStoreDBContainer = startLogStoreDB(network);
    private static final GenericContainer<?> logStoreContainer = startLogstoreServiceContainer(network, logStoreDBContainer);
    static final LogStoreServiceConnector logstoreServiceConnector = makeLogstoreConnector(logStoreContainer);
    private static final GenericContainer<?> artemisContainer = startArtemisContainer(network);
    private static final GenericContainer<?> jmsQueueServiceContainer = startJmsQueueServiceContainer(network);
    static final JmsQueueServiceConnector jmsQueueServiceConnector = makeJmsServiceConnector(jmsQueueServiceContainer);
    private static final DBCPostgreSQLContainer jobstoreDBContainer = startJobstoreDB(network);
    private static final GenericContainer<?> jobStoreServiceContainer = startJobStoreServiceContainer(network);
    static final JobStoreServiceConnector jobStoreServiceConnector = makeJobStoreConnector(jobStoreServiceContainer);

    private static JobStoreServiceConnector makeJobStoreConnector(GenericContainer<?> jobStoreContainer) {
        String url = "http://" + jobStoreContainer.getHost() + ":" + jobStoreContainer.getMappedPort(8080) + "/dataio/job-store-service";
        return new JobStoreServiceConnector(HttpClient.newClient(new ClientConfig().register(new JacksonFeature())), url);
    }

    private static JmsQueueServiceConnector makeJmsServiceConnector(GenericContainer<?> jmsQueueServiceContainer) {
        String jmsQueueServiceBaseurl = "http://" + jmsQueueServiceContainer.getHost() + ":" + jmsQueueServiceContainer.getMappedPort(8080);
        return new JmsQueueServiceConnector(HttpClient.newClient(new ClientConfig().register(new JacksonFeature())), jmsQueueServiceBaseurl);
    }

    private static LogStoreServiceConnector makeLogstoreConnector(GenericContainer<?> logstoreContainer) {
        String url = "http://" + logstoreContainer.getHost() + ":" + logstoreContainer.getMappedPort(8080) + "/dataio/log-store-service/";
        return new LogStoreServiceConnector(HttpClient.newClient(new ClientConfig().register(new JacksonFeature())), url);
    }

    private static DBCPostgreSQLContainer startLogStoreDB(Network network) {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer()
                .withNetwork(network)
                .withReuse(false)
                .withNetworkAliases("logstoreDB");
        container.start();
        container.exposeHostPort();
        return container;
    }

    private static DBCPostgreSQLContainer startJobstoreDB(Network network) {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withNetwork(network)
                .withReuse(false)
                .withNetworkAliases("jobstoreDB");
        container.start();
        container.exposeHostPort();
        populateJobstoreDB(container);
        return container;
    }

    private static WireMockServer startWireMockServer() {
        WireMockServer server = new WireMockServer(new WireMockConfiguration().dynamicPort());
        server.start();
        configureFor("localhost", server.port());
        Testcontainers.exposeHostPorts(server.port());
        LOGGER.info("Wiremock server at port:{}", server.port());
        return server;
    }

    private static GenericContainer<?> startArtemisContainer(Network network) {
        final GenericContainer<?> container = Containers.ARTEMIS.makeContainer()
                .withNetwork(network)
                .withNetworkAliases(ARTEMIS_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withExposedPorts(8161, 61616);
        container.start();
        return container;
    }


    private static GenericContainer<?> startLogstoreServiceContainer(Network network, DBCPostgreSQLContainer logStoreDBContainer) {
        final GenericContainer<?> container = Containers.LOG_STORE.makeContainer()
                .withNetwork(network)
                .withNetworkAliases(LOGSTORE)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOGSTORE_DB_URL", logStoreDBContainer.getPayaraDockerJdbcUrl())
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .waitingFor(Wait.forHttp("/dataio/log-store-service/status"))
                .withExposedPorts(8080);
        container.start();
        Connection logstoreDbConnection = connectToDB(logStoreDBContainer);
        populateLogstoreDB(logstoreDbConnection);
        return container;
    }

    private static GenericContainer<?> startJmsQueueServiceContainer(Network network) {
        final GenericContainer<?> container = Containers.JMS_QUEUE_SVC.makeContainer()
                .withNetwork(network)
                .withNetworkAliases(JMS_QUEUE_SERVICE_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "1G")
                .withEnv("ARTEMIS_MQ_HOST", ARTEMIS_ALIAS)
                .withExposedPorts(8080);
        container.start();
        return container;
    }

    private static GenericContainer<?> startJobStoreServiceContainer(Network network) {
        final GenericContainer<?> container = Containers.JOB_STORE.makeContainer()
                .withNetwork(network)
                .withNetworkAliases(JOBSTORE_SERVICE_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "4G")
                .withEnv("JOBSTORE_DB_URL", jobstoreDBContainer.getPayaraDockerJdbcUrl())
                .withEnv("ARTEMIS_MQ_HOST", ARTEMIS_ALIAS)
                .withEnv("ARTEMIS_ADMIN_PORT", "8161")
                .withEnv("ARTEMIS_USER", "admin")
                .withEnv("ARTEMIS_PASSWORD", "GoFish")
                .withEnv("FLOWSTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("FILESTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("LOGSTORE_URL", "http://" + LOGSTORE + ":8080/dataio/log-store-service/")
                .withEnv("RAWREPO_HARVESTER_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("TICKLE_REPO_HARVESTER_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("VIPCORE_ENDPOINT", "http://vipcore-dummy")
                .withEnv("MAIL_HOST", "webmail.dbc.dk")
                .withEnv("MAIL_USER", "mailuser")
                .withEnv("MAIL_FROM", "danbib")
                .withEnv("MAIL_TO_FALLBACK", "fallback")
                .withEnv("TZ", "Europe/Copenhagen")
                .withEnv("DEVELOPER", "on")
//                .withEnv("REMOTE_DEBUGGING_HOST", getDebuggingHost())
                .withExposedPorts(4848, 8080)
                .waitingFor(Wait.forHttp("/dataio/job-store-service/status")
                        .withReadTimeout(Duration.of(10, ChronoUnit.SECONDS)))
                .withStartupTimeout(Duration.ofMinutes(2));
        container.start();
        return container;
    }

    private static String getDebuggingHost() {
        try {
            String blah = InetAddress.getLocalHost().getHostAddress() + ":5005";
            return "192.168.0.88:5005";
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    static void populateJobstoreDB(DBCPostgreSQLContainer dbContainer) {
        try {
            migrateJobstore(dbContainer.datasource());

            final String sql = Files.readString(Paths.get("src/test/resources/sql/jobstore.sql"))
                    .replaceAll("__DATE_1__", oldJobDateTime.format(formatter))
                    .replaceAll("__DATE_2__", aLittleYoungerJobDateTime.format(formatter))
                    .replaceAll("__DATE_3__", jobFromTheDayBeforeThedayBeforeYesterday.format(formatter))
                    .replaceAll("__DATE_4__", jobFromToday.format(formatter));
            final PreparedStatement statement = connectToDB(dbContainer).prepareStatement(sql);
            statement.executeUpdate();

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void populateLogstoreDB(Connection connection) {
        try {
            final String sql = Files.readString(Paths.get("src/test/resources/sql/logstore.sql"))
                    .replaceAll("__DATE_1__", oldJobDateTime.format(formatter))
                    .replaceAll("__DATE_2__", aLittleYoungerJobDateTime.format(formatter))
                    .replaceAll("__DATE_3__", jobFromTheDayBeforeThedayBeforeYesterday.format(formatter))
                    .replaceAll("__DATE_4__", jobFromToday.format(formatter));
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }


    static Connection connectToDB(DBCPostgreSQLContainer container) {
        try {
            return container.createConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void emptyQueues() {
        Arrays.stream(JmsQueueServiceConnector.Queue.values()).forEach(jmsQueueServiceConnector::emptyQueue);
    }

    static void migrateJobstore(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .locations("filesystem:../../job-store-service/war/target/classes/db/migration")
                .table("schema_version_2")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .dataSource(dataSource)
                .load();
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.info("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();
    }
}
