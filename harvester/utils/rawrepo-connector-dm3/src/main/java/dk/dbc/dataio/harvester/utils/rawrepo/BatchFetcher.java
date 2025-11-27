package dk.dbc.dataio.harvester.utils.rawrepo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public interface BatchFetcher<T> {
    int batch(int limit, Consumer<List<T>> batchConsumer) throws SQLException;
    default void commit(Connection connection) throws SQLException {
        if(!connection.getAutoCommit()) connection.commit();
    }
}
