/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractJobStoreServiceContainerTest {
    static final Connection jobStoreDbConnection;

    static {
        jobStoreDbConnection = connectToJobStoreDB();
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
