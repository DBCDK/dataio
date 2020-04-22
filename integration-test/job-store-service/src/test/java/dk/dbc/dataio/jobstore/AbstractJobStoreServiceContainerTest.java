/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.dataio.commons.testcontainers.Containers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class AbstractJobStoreServiceContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobStoreServiceContainerTest.class);

    static final Connection jobStoreDbConnection;

    private static final String OPENMQ_ALIAS = "dataio-openmq";

    private static WireMockServer wireMockServer;
    private static GenericContainer openmqContainer;

    static {
        jobStoreDbConnection = connectToJobStoreDB();

        wireMockServer = startWireMockServer();

        final Network network = Network.newNetwork();
        openmqContainer = startOpenmqContainer(network);
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
}
