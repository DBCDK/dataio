package dk.dbc.dataio.jobstore;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jms.JmsQueueServiceConnector;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
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

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static dk.dbc.dataio.commons.types.JobSpecification.JOB_EXPIRATION_AGE_IN_DAYS;

public abstract class AbstractJobStoreServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobStoreServiceContainerTest.class);
    static final Connection logstoreDbConnection;
    static final JobStoreServiceConnector jobStoreServiceConnector;
    static final LogStoreServiceConnector logStoreServiceConnector;
    static final JmsQueueServiceConnector jmsQueueServiceConnector;

    private static final String ARTEMIS_ALIAS = "dataio-artemis";
    private static final String LOGSTORE = "dataio-logstore";
    private static final String JMS_QUEUE_SERVICE_ALIAS = "dataio-jms-queue-service";
    private static final String JOBSTORE_SERVICE_ALIAS = "dataio-jobstore-service";

    private static WireMockServer wireMockServer;
    static GenericContainer<?> artemisContainer;
    private static GenericContainer<?> jmsQueueServiceContainer;
    private static GenericContainer<?> jobStoreServiceContainer;
    private static DBCPostgreSQLContainer logstoreDBContainer;
    private static DBCPostgreSQLContainer jobstoreDBContainer;
    private static GenericContainer<?> logstoreContainer;


    private static final LocalDateTime oldJobDateTime = LocalDateTime.now()
            .minus(JOB_EXPIRATION_AGE_IN_DAYS + 1, ChronoUnit.DAYS);

    private static final LocalDateTime aLittleYoungerJobDateTime = LocalDateTime.now()
            .minus(JOB_EXPIRATION_AGE_IN_DAYS - 1, ChronoUnit.DAYS);
    private static final LocalDateTime jobFromTheDayBeforeYesterday = LocalDateTime.now()
            .minus(2, ChronoUnit.DAYS);
    private static final LocalDateTime jobFromToday = LocalDateTime.now()
            .minus(2, ChronoUnit.HOURS);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static {
        wireMockServer = startWireMockServer();

        final Network network = Network.newNetwork();
        artemisContainer = startArtemisContainer(network);
        jmsQueueServiceContainer = startJmsQueueServiceContainer(network);

        jobstoreDBContainer = new DBCPostgreSQLContainer()
                .withNetwork(network).withReuse(false);
        jobstoreDBContainer.start();
        jobstoreDBContainer.exposeHostPort();
        new DatabaseMigrator()
                .withDataSource(jobstoreDBContainer.datasource())
                .onStartup();
        populateJobstoreDB(connectToJobstoreDB());


        logstoreDBContainer = new DBCPostgreSQLContainer().withNetwork(network).withReuse(false);
        logstoreDBContainer.start();
        logstoreDBContainer.exposeHostPort();


        logstoreContainer = startLogstoreServiceContainer(network);
        logstoreDbConnection = connectToLogstoreDB();
        populateLogstoreDB(logstoreDbConnection);

        jobStoreServiceContainer = startJobStoreServiceContainer(network);

        final String jobStoreServiceBaseurl = "http://" + jobStoreServiceContainer.getContainerIpAddress() +
                ":" + jobStoreServiceContainer.getMappedPort(8080) +
                System.getProperty("jobstore.it.service.context");
        jobStoreServiceConnector = new JobStoreServiceConnector(
                HttpClient.newClient(new ClientConfig().register(new JacksonFeature())),
                jobStoreServiceBaseurl);

        final String jmsQueueServiceBaseurl = "http://" + jmsQueueServiceContainer.getContainerIpAddress() +
                ":" + jmsQueueServiceContainer.getMappedPort(8080);
        jmsQueueServiceConnector = new JmsQueueServiceConnector(
                HttpClient.newClient(new ClientConfig().register(new JacksonFeature())),
                jmsQueueServiceBaseurl);

        final String logstoreServiceBaseurl = "http://" + logstoreContainer.getContainerIpAddress() +
                ":" + logstoreContainer.getMappedPort(8080) + "/dataio/log-store-service/";
        logStoreServiceConnector = new LogStoreServiceConnector(HttpClient.newClient(new ClientConfig().register(new JacksonFeature())),
                logstoreServiceBaseurl);
    }

    private static WireMockServer startWireMockServer() {
        wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        Testcontainers.exposeHostPorts(wireMockServer.port());
        LOGGER.info("Wiremock server at port:{}", wireMockServer.port());
//        URL sinks = AbstractJobStoreServiceContainerTest.class.getClassLoader().getResource("flowstore/sinks.json");
//        try {
//            String sinksResponse = Files.readString(Path.of(sinks.toURI()));
//            wireMockServer.stubFor(get("/dataio/flow-store-service/sinks").willReturn(ResponseDefinitionBuilder.okForJson(sinksResponse)));
//        } catch (IOException | URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
        return wireMockServer;
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


    private static GenericContainer<?> startLogstoreServiceContainer(Network network) {
        final GenericContainer<?> container = Containers.LOG_STORE.makeContainer()
                .withNetwork(network)
                .withNetworkAliases(LOGSTORE)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOGSTORE_DB_URL", logstoreDBContainer.getPayaraDockerJdbcUrl())
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .waitingFor(Wait.forHttp("/dataio/log-store-service/status"))
                .withExposedPorts(8080);
        container.start();
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
//                .withEnv("REMOTE_DEBUGGING_HOST", getDebuggingHost())
                .withExposedPorts(4848, 8080)
                .waitingFor(Wait.forHttp(System.getProperty("jobstore.it.service.context") + "/status"))
                .withStartupTimeout(Duration.ofMinutes(10));
        container.start();
        return container;
    }

    private static String getDebuggingHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":5005";
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    static void populateJobstoreDB(Connection connection) {
        try {

            final String sql = Files.readString(Paths.get("src/test/resources/sql/jobstore.sql"))
                    .replaceAll("__DATE_1__", oldJobDateTime.format(formatter))
                    .replaceAll("__DATE_2__", aLittleYoungerJobDateTime.format(formatter))
                    .replaceAll("__DATE_3__", jobFromTheDayBeforeYesterday.format(formatter))
                    .replaceAll("__DATE_4__", jobFromToday.format(formatter));
            final PreparedStatement statement = connection.prepareStatement(sql);
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
                    .replaceAll("__DATE_3__", jobFromTheDayBeforeYesterday.format(formatter))
                    .replaceAll("__DATE_4__", jobFromToday.format(formatter));
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }


    static Connection connectToLogstoreDB() {
        try {
            return logstoreDBContainer.createConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static Connection connectToJobstoreDB() {
        try {
            return jobstoreDBContainer.createConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void emptyQueues() {
        jmsQueueServiceConnector.emptyQueue(JmsQueueServiceConnector.Queue.PROCESSING);
        jmsQueueServiceConnector.emptyQueue(JmsQueueServiceConnector.Queue.SINK);
    }
}
