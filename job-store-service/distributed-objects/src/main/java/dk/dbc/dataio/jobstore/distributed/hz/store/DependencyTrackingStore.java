package dk.dbc.dataio.jobstore.distributed.hz.store;

import com.hazelcast.map.MapStore;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.tools.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.distributed.tools.StringSetConverter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class DependencyTrackingStore implements MapStore<TrackingKey, DependencyTracking> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingStore.class);
    private static final KeySetJSONBConverter KEY_SET_CONVERTER = new KeySetJSONBConverter();
    private static final StringSetConverter STRING_SET_CONVERTER = new StringSetConverter();
    public static final String DS_JNDI = "jdbc/dataio/jobstore";
    private final DataSource dataSource;
    private static MetricRegistry metricRegistry;

    private static final String UPSERT = "insert into dependencytracking(jobid, chunkid, sinkid, status, waitingon, matchkeys, priority, submitter, lastmodified, retries) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict on constraint dependencytracking_pkey do " +
            "update set status=excluded.status, waitingon=excluded.waitingon, priority=excluded.priority, lastmodified=excluded.lastmodified, retries=excluded.retries";
    private static final String SELECT = "select * from dependencytracking where jobid=? and chunkid=?";

    public DependencyTrackingStore() {
        this(lookupDataSource());
    }

    public DependencyTrackingStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static synchronized void setMetricRegistry(MetricRegistry metricRegistry) {
        DependencyTrackingStore.metricRegistry = metricRegistry;
    }

    @Override
    public void store(TrackingKey key, DependencyTracking dte) {
        LOGGER.info("Storing: {}", key);
        store(UPSERT, "store", ps -> {
            setRow(ps, dte);
            ps.executeUpdate();
        });
    }

    @Override
    public void storeAll(Map<TrackingKey, DependencyTracking> map) {
        LOGGER.info("Storing {} trackers", map.size());
        store(UPSERT, "store_all", ps -> {
            for (DependencyTracking dte : map.values()) {
                setRow(ps, dte);
                ps.addBatch();
            }
            ps.executeBatch();
        });
    }

    @Override
    public void delete(TrackingKey key) {
        deleteAll(List.of(key));
    }

    @Override
    public void deleteAll(Collection<TrackingKey> keys) {
        String sql = "delete from dependencytracking where jobid = ? and chunkid = ?";
        store(sql, "delete_all", ps -> {
            for (TrackingKey key : keys) {
                ps.setInt(1, key.getJobId());
                ps.setInt(2, key.getChunkId());
                ps.addBatch();
            }
            ps.executeBatch();
        });
    }

    @Override
    public DependencyTracking load(TrackingKey key) {
        LOGGER.info("Loading trackers: {}", key);
        return fetch(SELECT, "load", ps -> {
            setKey(ps, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new DependencyTracking(rs);
            return null;
        });
    }

    @Override
    public Map<TrackingKey, DependencyTracking> loadAll(Collection<TrackingKey> keys) {
        LOGGER.info("Loading {} trackers", keys.size());
        String sql = null;
        Map<Integer, List<TrackingKey>> jobs = keys.stream().collect(Collectors.groupingBy(TrackingKey::getJobId));
        try (Connection c = dataSource.getConnection()) {
            Map<TrackingKey, DependencyTracking> entities = new HashMap<>();
            for (Map.Entry<Integer, List<TrackingKey>> job : jobs.entrySet()) {
                String chunks = job.getValue().stream().mapToInt(TrackingKey::getChunkId).mapToObj(Integer::toString).collect(Collectors.joining(", "));
                sql = "select * from dependencytracking where jobid=? and chunkid in (" + chunks + ")";
                try(PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, job.getKey());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        DependencyTracking entity = new DependencyTracking(rs);
                        entities.put(entity.getKey(), entity);
                    }
                }
            }
            return entities;
        } catch (SQLException e) {
            LOGGER.error("Statement failed {}", sql);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<TrackingKey> loadAllKeys() {
        String sql = "select jobid, chunkid from dependencytracking";
        return fetch(sql, "load_all_keys", ps -> {
            ResultSet rs = ps.executeQuery();
            List<TrackingKey> keys = new ArrayList<>();
            while (rs.next()) keys.add(new TrackingKey(rs.getInt("jobid"), rs.getInt("chunkid")));
            LOGGER.info("Load all keys found " + keys.size() + " trackers to load");
            return keys;
        });
    }

    private static void setRow(PreparedStatement ps, DependencyTracking dte) throws SQLException {
        setKey(ps, dte.getKey());
        ps.setInt(3, dte.getSinkId());
        ps.setInt(4, dte.getStatus().value);
        ps.setObject(5, KEY_SET_CONVERTER.convertToDatabaseColumn(dte.getWaitingOn()));
        ps.setObject(6, STRING_SET_CONVERTER.convertToDatabaseColumn(dte.getMatchKeys()));
        ps.setInt(7, dte.getPriority());
        ps.setInt(8, dte.getSubmitter());
        ps.setTimestamp(9, new Timestamp(dte.getLastModified().toEpochMilli()));
        ps.setInt(10, dte.getRetries());
    }

    private static void setKey(PreparedStatement ps, TrackingKey key) throws SQLException {
        ps.setInt(1, key.getJobId());
        ps.setInt(2, key.getChunkId());
    }

    private <T> T fetch(String sql, String metric, SqlFunction<T> block) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            return time(metric, () -> block.accept(ps));
        } catch (Exception e) {
            LOGGER.error("Statement failed {}", sql);
            throw new IllegalStateException(e);
        }
    }

    private void store(String sql, String metricName, SqlConsumer block) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            time(metricName, (Callable<Void>) () -> {
                block.accept(ps);
                return null;
            });
        } catch (Exception e) {
            LOGGER.error("Map loader store failed while executing: {}", sql);
            throw new IllegalStateException("Map loader store failed while executing: " + sql, e);
        }
    }

    private static DataSource lookupDataSource() {
        try {
            LOGGER.info("Initializing map loader");
            return (DataSource) new InitialContext().lookup(DS_JNDI);
        } catch (NamingException e) {
            LOGGER.error("Unable to lookup datasource {}", DS_JNDI, e);
            return null;
        }
    }

    private <T> T time(String metricName, Callable<T> c) throws Exception {
        if (metricRegistry != null) return metricRegistry.timer("dataio_dependency_store_" + metricName).time(c);
        return c.call();
    }

    public interface SqlConsumer {
        void accept(PreparedStatement ps) throws SQLException;
    }

    public interface SqlFunction<T> {
        T accept(PreparedStatement ps) throws SQLException;
    }
}
