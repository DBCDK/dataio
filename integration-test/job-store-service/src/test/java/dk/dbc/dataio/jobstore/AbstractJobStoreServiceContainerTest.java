/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class AbstractJobStoreServiceContainerTest {
    static final Connection jobStoreDbConnection;

    private static WireMockServer wireMockServer = startWireMockServer();

    static {
        jobStoreDbConnection = connectToJobStoreDB();
    }

    private static WireMockServer startWireMockServer() {
        final int port = Integer.parseInt(System.getProperty("jobstore.it.wiremock.port"));
        final WireMockServer wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        return wireMockServer;
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
