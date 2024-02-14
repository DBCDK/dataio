package dk.dbc.dataio.jobstore.service.dependencytracking.loader;

import com.hazelcast.map.MapStore;
import dk.dbc.commons.jpa.converter.IntegerArrayToPgIntArrayConverter;
import dk.dbc.dataio.jobstore.service.entity.DependencyTracking;
import dk.dbc.dataio.jobstore.service.entity.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.service.entity.StringSetConverter;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DependencyTrackingLoader implements MapStore<DependencyTracking.Key, DependencyTracking> {
    private static final KeySetJSONBConverter KEY_SET_CONVERTER = new KeySetJSONBConverter();
    private static final StringSetConverter STRING_SET_CONVERTER = new StringSetConverter();
    private static final IntegerArrayToPgIntArrayConverter INT_ARRAY_CONVERTER = new IntegerArrayToPgIntArrayConverter();
    private static final String DS_JNDI = "jdbc/dataio/jobstore";
    private DataSource dataSource;

    private static final String UPSERT = "insert into dependencytracking(jobid, chunkid, sinkid, status, waitingon, matchkeys, priority, hashes, submitter, lastmodified, retries) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict on constraint dependencytracking_pkey do " +
            "update set status=excluded.status, waitingon=excluded.waitingon, matchkeys=excluded.matchkeys, priority=excluded.priority, hashes=excluded.hashes, submitter=excluded.submitter, lastmodified=excluded.lastmodified, retries=excluded.retries";
    private static final String SELECT = "select * from dependencytracking where jobid=? and chunkid=?";

    public DependencyTrackingLoader() throws NamingException {
        this((DataSource) new InitialContext().lookup(DS_JNDI));
    }

    public DependencyTrackingLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void store(DependencyTracking.Key key, DependencyTracking dte) {
        store(UPSERT, ps -> {
            setRow(ps, dte);
            ps.executeUpdate();
        });
    }


    @Override
    public void storeAll(Map<DependencyTracking.Key, DependencyTracking> map) {
        store(UPSERT, ps -> {
            for (DependencyTracking dte : map.values()) {
                setRow(ps, dte);
                ps.addBatch();
            }
            ps.executeBatch();
        });
    }

    @Override
    public void delete(DependencyTracking.Key key) {
        deleteAll(List.of(key));
    }

    @Override
    public void deleteAll(Collection<DependencyTracking.Key> keys) {
        String sql = "delete from dependencytracking where jobid = ? and chunkid = ?";
        store(sql, ps -> {
            for (DependencyTracking.Key key : keys) {
                ps.setInt(1, key.getJobId());
                ps.setInt(2, key.getChunkId());
                ps.addBatch();
            }
            ps.executeBatch();
        });
    }

    @Override
    public DependencyTracking load(DependencyTracking.Key key) {
        return fetch(SELECT, ps -> {
            setKey(ps, key);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) return new DependencyTracking(rs);
            return null;
        });
    }

    @Override
    public Map<DependencyTracking.Key, DependencyTracking> loadAll(Collection<DependencyTracking.Key> keys) {
        Map<Integer, List<DependencyTracking.Key>> jobs = keys.stream().collect(Collectors.groupingBy(DependencyTracking.Key::getJobId));
        String sql = "select * from dependencytracking where jobid=? and chunkid in [?]";
        return fetch(SELECT, ps -> {
            Map<DependencyTracking.Key, DependencyTracking> entities = new HashMap<>();
            for (Integer jobId : jobs.keySet()) {
                String chunks = jobs.get(jobId).stream().mapToInt(DependencyTracking.Key::getChunkId).mapToObj(Integer::toString).collect(Collectors.joining(", "));
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
    public Iterable<DependencyTracking.Key> loadAllKeys() {
        String sql = "select jobid, chunkid from dependencytracking";
        return fetch(sql, ps -> {
            ResultSet rs = ps.executeQuery();
            List<DependencyTracking.Key> keys = new ArrayList<>();
            while(rs.next()) keys.add(new DependencyTracking.Key(rs.getInt("jobid"), rs.getInt("chunkid")));
            return keys;
        });
    }

    private static void setRow(PreparedStatement ps, DependencyTracking dte) throws SQLException {
        setKey(ps, dte.getKey());
        ps.setInt(3, dte.getSinkid());
        ps.setInt(4, dte.getStatus().value);
        ps.setObject(5, KEY_SET_CONVERTER.convertToDatabaseColumn(dte.getWaitingOn()));
        ps.setObject(6, STRING_SET_CONVERTER.convertToDatabaseColumn(dte.getMatchKeys()));
        ps.setInt(7, dte.getPriority());
        ps.setObject(8, INT_ARRAY_CONVERTER.convertToDatabaseColumn(dte.getHashes()));
        ps.setInt(9, dte.getSubmitterNumber());
        ps.setTimestamp(10, dte.getLastModified());
        ps.setInt(11, dte.getRetries());
    }

    private static void setKey(PreparedStatement ps, DependencyTracking.Key key) throws SQLException {
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
            throw new RuntimeException(e);
        }
    }

    public interface SqlConsumer {
        void accept(PreparedStatement ps) throws SQLException;
    }

    public interface SqlFunction<T> {
        T accept(PreparedStatement ps) throws SQLException;
    }
}
