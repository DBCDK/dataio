package dk.dbc.dataio.harvester;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class RawRepoUtil {
    private static final long SLEEP_INTERVAL_IN_MS = 1000;

    private RawRepoUtil() { }

    public static Connection newRawRepoConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection(
                String.format("jdbc:postgresql://localhost:%s/%s", System.getProperty("postgresql.port"), System.getProperty("rawrepo.db.name")),
                System.getProperty("user.name"), System.getProperty("user.name"));
        conn.setAutoCommit(true);
        return conn;
    }

    public static void setupRawRepo() throws SQLException, ClassNotFoundException {
        try (final Connection connection = newRawRepoConnection()) {
            try {
                JDBCUtil.update(connection, "INSERT INTO queueworkers(worker) VALUES(?)", "fbs-sync");
                JDBCUtil.update(connection, "INSERT INTO queuerules(provider, worker, changed, leaf) VALUES(?, ?, ?, ?)", "opencataloging-update", "fbs-sync", "Y", "A");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearRawRepo() throws SQLException, ClassNotFoundException {
        try (final Connection connection = newRawRepoConnection()) {
            JDBCUtil.update(connection, "DELETE FROM relations");
            JDBCUtil.update(connection, "DELETE FROM records");
            JDBCUtil.update(connection, "DELETE FROM queue");
        }
    }

    public static void awaitQueueSize(String consumer, long expectedQueueSize, long maxWaitInMs) throws SQLException, ClassNotFoundException {
        long remainingWaitInMs = maxWaitInMs;
        long actualQueueSize = getQueueSize(consumer);
        while (actualQueueSize != expectedQueueSize && remainingWaitInMs > 0) {
            try {
                Thread.sleep(SLEEP_INTERVAL_IN_MS);
                remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                actualQueueSize = getQueueSize(consumer);
            } catch (InterruptedException e) {
                break;
            }
        }
        if (actualQueueSize != expectedQueueSize) {
            throw new IllegalStateException(String.format("Expected size %d of queue %s differs from actual size %d",
                    expectedQueueSize, consumer, actualQueueSize));
        }
    }

    public static long getQueueSize(String consumer) throws SQLException, ClassNotFoundException {
        try (final Connection connection = newRawRepoConnection()) {
            final List<List<Object>> result = JDBCUtil.queryForRowLists(connection, "SELECT COUNT(*) FROM queue WHERE worker=?", consumer);
            return (long) result.get(0).get(0);
        }
    }
}
