/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jms.JmsQueueServiceConnector;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class AbstractJobStoreServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobStoreServiceContainerTest.class);

    static {
        Testcontainers.exposeHostPorts(Integer.parseInt(
                System.getProperty("jobstore.it.postgresql.port")));
        Testcontainers.exposeHostPorts(Integer.parseInt(
                System.getProperty("jobstore.it.wiremock.port")));
    }

    static final Connection jobStoreDbConnection;
    static final Connection logstoreDbConnection;
    static final JobStoreServiceConnector jobStoreServiceConnector;
    static final LogStoreServiceConnector logStoreServiceConnector;
    static final JmsQueueServiceConnector jmsQueueServiceConnector;

    private static final String OPENMQ_ALIAS = "dataio-openmq";
    private static final String LOGSTORE = "dataio-logstore";
    private static final String JMS_QUEUE_SERVICE_ALIAS = "dataio-jms-queue-service";
    private static final String JOBSTORE_SERVICE_ALIAS = "dataio-jobstore-service";

    private static WireMockServer wireMockServer;
    private static GenericContainer openmqContainer;
    private static GenericContainer jmsQueueServiceContainer;
    private static GenericContainer jobStoreServiceContainer;
    private static DBCPostgreSQLContainer logstoreDBContainer;
    private static GenericContainer logstoreContainer;

    static {
        jobStoreDbConnection = connectToJobStoreDB();

        wireMockServer = startWireMockServer();

        final Network network = Network.newNetwork();
        openmqContainer = startOpenmqContainer(network);
        jmsQueueServiceContainer = startJmsQueueServiceContainer(network);
        jobStoreServiceContainer = startJobStoreServiceContainer(network);
        populateJobstoreDB(jobStoreDbConnection);
        logstoreDBContainer = new DBCPostgreSQLContainer()
                .withNetwork(network);
        logstoreDBContainer.start();
        logstoreDBContainer.exposeHostPort();



        logstoreContainer = startLogstoreServiceContainer(network);
        logstoreDbConnection = connectToLogstoreDB();
        populateLogstoreDB(logstoreDbConnection);


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
        final int port = Integer.parseInt(System.getProperty("jobstore.it.wiremock.port"));
        final WireMockServer wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        return wireMockServer;
    }

    private static GenericContainer startOpenmqContainer(Network network) {
        final GenericContainer container = Containers.openmqContainer()
                .withNetwork(network)
                .withNetworkAliases(OPENMQ_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withExposedPorts(7676);
        container.start();
        return container;
    }


    private static GenericContainer startLogstoreServiceContainer(Network network) {
        final GenericContainer container = Containers.logstoreContainer()
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

    private static GenericContainer startJmsQueueServiceContainer(Network network) {
        final GenericContainer container = Containers.jmsQueueServiceContainer()
                .withNetwork(network)
                .withNetworkAliases(JMS_QUEUE_SERVICE_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("OPENMQ_SERVER", OPENMQ_ALIAS + ":7676")
                .withExposedPorts(8080);
        container.start();
        return container;    
    }

    private static GenericContainer startJobStoreServiceContainer(Network network) {
        final GenericContainer container = Containers.jobstoreServiceContainer()
                .withNetwork(network)
                .withNetworkAliases(JOBSTORE_SERVICE_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("JAVA_MAX_HEAP_SIZE", "4G")
                .withEnv("JOBSTORE_DB_URL", String.format("%s:%s@host.testcontainers.internal:%s/%s",
                        System.getProperty("user.name"),
                        System.getProperty("user.name"),
                        System.getProperty("jobstore.it.postgresql.port"),
                        System.getProperty("jobstore.it.postgresql.dbname")))
                .withEnv("OPENMQ_SERVER", OPENMQ_ALIAS + ":7676")
                .withEnv("FLOWSTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("FILESTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("LOGSTORE_URL", "http://"+LOGSTORE+":8080/dataio/log-store-service/")
                .withEnv("RAWREPO_HARVESTER_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("TICKLE_REPO_HARVESTER_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("VIPCORE_ENDPOINT", "http://vipcore-dummy")
                .withEnv("MAIL_HOST", "webmail.dbc.dk")
                .withEnv("MAIL_USER", "mailuser")
                .withEnv("MAIL_FROM", "danbib")
                .withEnv("MAIL_TO_FALLBACK", "fallback")
                .withEnv("TZ", "Europe/Copenhagen")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp(System.getProperty("jobstore.it.service.context") + "/status"))
                .withStartupTimeout(Duration.ofMinutes(5));
        container.start();
        return container;
    }

    static Connection connectToJobStoreDB() {
        try {
            Class.forName("org.postgresql.Driver");
            final String dbUrl = String.format("jdbc:postgresql://localhost:%s/%s",
                    System.getProperty("jobstore.it.postgresql.port"),
                    System.getProperty("jobstore.it.postgresql.dbname"));
            final Connection connection = DriverManager.getConnection(dbUrl,
                    System.getProperty("user.name"),
                    System.getProperty("user.name"));
            connection.setAutoCommit(true);
            return connection;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void populateJobstoreDB(Connection connection) {
        try {
            final LocalDateTime oldJobDateTime = LocalDateTime.now()
                    .minus(5, ChronoUnit.YEARS)
                    .minus(1, ChronoUnit.DAYS);
            final LocalDateTime aLittleYoungerJobDateTime = LocalDateTime.now()
                    .minus(5, ChronoUnit.YEARS)
                    .plus(20, ChronoUnit.DAYS);


            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            final String sql = new String(Files.readAllBytes(Paths.get("src/test/resources/sql/jobstore.sql")), StandardCharsets.UTF_8)
                    .replaceAll("__DATE_1__", oldJobDateTime.format(formatter))
                    .replaceAll("__DATE_2__", aLittleYoungerJobDateTime.format(formatter));
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void populateLogstoreDB(Connection connection) {
        try {
            final LocalDateTime oldJobDateTime = LocalDateTime.now()
                    .minus(5, ChronoUnit.YEARS)
                    .minus(1, ChronoUnit.DAYS);
            final LocalDateTime aLittleYoungerJobDateTime = LocalDateTime.now()
                    .minus(5, ChronoUnit.YEARS)
                    .plus(1, ChronoUnit.DAYS);


            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            final String sql = new String(Files.readAllBytes(Paths.get("src/test/resources/sql/logstore.sql")), StandardCharsets.UTF_8)
                    .replaceAll("__DATE_1__", oldJobDateTime.format(formatter))
                    .replaceAll("__DATE_2__", aLittleYoungerJobDateTime.format(formatter));
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();

        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }


    static Connection connectToLogstoreDB() {
        try {
            return  logstoreDBContainer.createConnection();
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
