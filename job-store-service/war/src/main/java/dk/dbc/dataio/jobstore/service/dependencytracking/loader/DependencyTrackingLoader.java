package dk.dbc.dataio.jobstore.service.dependencytracking.loader;

import com.hazelcast.map.MapStore;
import dk.dbc.commons.jpa.converter.IntegerArrayToPgIntArrayConverter;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;
import dk.dbc.dataio.jobstore.service.entity.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.service.entity.StringSetConverter;
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
import java.util.stream.Collectors;

public class DependencyTrackingLoader implements MapStore<TrackingKey, DependencyTracking> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyTrackingLoader.class);
    private static final KeySetJSONBConverter KEY_SET_CONVERTER = new KeySetJSONBConverter();
    private static final StringSetConverter STRING_SET_CONVERTER = new StringSetConverter();
    private static final IntegerArrayToPgIntArrayConverter INT_ARRAY_CONVERTER = new IntegerArrayToPgIntArrayConverter();
    private static final String DS_JNDI = "jdbc/dataio/jobstore";
    private final DataSource dataSource;

    private static final String UPSERT = "insert into dependencytracking(jobid, chunkid, sinkid, status, waitingon, matchkeys, priority, hashes, submitter, lastmodified, retries) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict on constraint dependencytracking_pkey do " +
            "update set status=excluded.status, waitingon=excluded.waitingon, matchkeys=excluded.matchkeys, priority=excluded.priority, hashes=excluded.hashes, submitter=excluded.submitter, lastmodified=excluded.lastmodified, retries=excluded.retries";
    private static final String SELECT = "select * from dependencytracking where jobid=? and chunkid=?";

    public DependencyTrackingLoader() {
        this(lookupDataSource());
    }

    public DependencyTrackingLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void store(TrackingKey key, DependencyTracking dte) {
        store(UPSERT, ps -> {
            setRow(ps, dte);
            ps.executeUpdate();
        });
    }


    @Override
    public void storeAll(Map<TrackingKey, DependencyTracking> map) {
        store(UPSERT, ps -> {
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
        store(sql, ps -> {
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
        return fetch(SELECT, ps -> {
            setKey(ps, key);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) return new DependencyTracking(rs);
            return null;
        });
    }

    @Override
    public Map<TrackingKey, DependencyTracking> loadAll(Collection<TrackingKey> keys) {
        Map<Integer, List<TrackingKey>> jobs = keys.stream().collect(Collectors.groupingBy(TrackingKey::getJobId));
        String sql = "select * from dependencytracking where jobid=? and chunkid in [?]";
        return fetch(sql, ps -> {
            Map<TrackingKey, DependencyTracking> entities = new HashMap<>();
            for (Integer jobId : jobs.keySet()) {
                String chunks = jobs.get(jobId).stream().mapToInt(TrackingKey::getChunkId).mapToObj(Integer::toString).collect(Collectors.joining(", "));
                ps.setInt(1, jobId);
                ps.setString(2, chunks);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    DependencyTracking entity = new DependencyTracking(rs);
                    entities.put(entity.getKey(), entity);
                }
            }
            return entities;
        });
    }

    @Override
    public Iterable<TrackingKey> loadAllKeys() {
        String sql = "select jobid, chunkid from dependencytracking";
        return fetch(sql, ps -> {
            ResultSet rs = ps.executeQuery();
            List<TrackingKey> keys = new ArrayList<>();
            while(rs.next()) keys.add(new TrackingKey(rs.getInt("jobid"), rs.getInt("chunkid")));
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
        ps.setObject(8, INT_ARRAY_CONVERTER.convertToDatabaseColumn(dte.getHashes()));
        ps.setInt(9, dte.getSubmitterNumber());
        ps.setTimestamp(10, new Timestamp(dte.getLastModified().toEpochMilli()));
        ps.setInt(11, dte.getRetries());
    }

    private static void setKey(PreparedStatement ps, TrackingKey key) throws SQLException {
        ps.setInt(1, key.getJobId());
        ps.setInt(2, key.getChunkId());
    }

    private <T> T fetch(String sql, SqlFunction<T> block) {
        try(Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            return block.accept(ps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void store(String sql, SqlConsumer block) {
        try(Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            block.accept(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Map loader store failed while executing: " + sql, e);
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

    public interface SqlConsumer {
        void accept(PreparedStatement ps) throws SQLException;
    }

    public interface SqlFunction<T> {
        T accept(PreparedStatement ps) throws SQLException;
    }
}
