package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.pgqueue.consumer.BasicHarvester;
import dk.dbc.pgqueue.consumer.JobMetaData;
import dk.dbc.pgqueue.consumer.JobStatements;
import dk.dbc.pgqueue.consumer.JobWithMetaData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BatchFetcherImpl<T> extends JobStatements<T> implements BatchFetcher<T> {
    private final String consumerId;

    public BatchFetcherImpl(String consumerId, BasicHarvester<T> harvester) {
        super(harvester);
        this.consumerId = consumerId;
    }

    @Override
    public int batch(int limit, Consumer<List<T>> batchConsumer) throws SQLException {
        try {
            setupConnection();
            return handle(limit, Objects.requireNonNull(batchConsumer));
        } catch (RuntimeException e) {
            connection.rollback();
            return 0;
        }  finally {
            releaseConnection();
        }
    }

    private int handle(int limit, Consumer<List<T>> batchConsumer) throws SQLException {
        List<JobWithMetaData<T>> list = new ArrayList<>();
        try(PreparedStatement ps = getBatchSelectStmt(consumerId, limit);
            ResultSet rs = ps.executeQuery()) {
            while(rs.next()) list.add(new JobWithMetaData<>(rs, 1, harvester.settings.storageAbstraction));
            if(list.isEmpty()) return 0;
            List<T> result = list.stream()
                    .sorted(Comparator.comparing(JobMetaData::getDequeueAfter))
                    .map(JobWithMetaData::getActualJob)
                    .toList();
            batchConsumer.accept(result);
            commit(connection);
            return result.size();
        }
    }
}
