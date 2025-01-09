package dk.dbc.dataio.jobstore.distributed.hz.store;

import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.hazelcast.test.HazelcastTestSupport.assertContainsAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DependencyTrackingStoreTest implements PostgresContainerJPAUtils {
    private static final DataSource datasource = dbContainer.bindDatasource(DependencyTrackingStore.DS_JNDI).datasource();
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("jobstoreIT", dbContainer.entityManagerProperties());
    private static DependencyTrackingStore trackingStore;

    @BeforeAll
    public static void createDb() throws NamingException {
        new DatabaseMigrator().withDataSource(datasource).onStartup();
        trackingStore = new DependencyTrackingStore();
    }

    @Test
    void loadAllKeys() {
        initialLoad();
        int count = 0;
        Set<TrackingKey> keys = new HashSet<>();
        for (TrackingKey key : trackingStore.loadAllKeys()) {
            count++;
            keys.add(key);
        }
        assertEquals(11, count, "We should have loaded 11 keys");
        assertContainsAll(keys, Trackers.getMap().keySet());
    }

    @Test
    void load() {
        initialLoad();
        TrackingKey key = Trackers.TRACKER_1_0.key();
        DependencyTracking dt = trackingStore.load(key);
        assertEquals(Trackers.TRACKER_1_0.get(), dt);
    }

    @Test
    void loadAll() {
        initialLoad();
        Set<TrackingKey> keys = Trackers.getMap().keySet();
        Map<TrackingKey, DependencyTracking> trackingMap = trackingStore.loadAll(keys);
        assertEquals(keys, trackingMap.keySet(), "We should have 2 trackers");
    }

    @Test
    void delete() {
        initialLoad();
        TrackingKey key = new TrackingKey(1, 2);
        trackingStore.delete(key);
        assertNull(trackingStore.load(key), "The deleted tracker should not exist");
    }

    @Test
    void deleteAll() {
        Set<TrackingKey> keys = Trackers.getMap().keySet();
        trackingStore.deleteAll(keys);
        keys.forEach(k -> assertNull(trackingStore.load(k), "Tracker " + k + " should not exist"));
    }

    @Test
    void store() {
        initialLoad();
        DependencyTracking dt = Trackers.TRACKER_1_0.get();
        trackingStore.delete(Trackers.TRACKER_1_0.key());
        trackingStore.store(Trackers.TRACKER_1_0.key(), dt);
        assertEquals(dt, trackingStore.load(dt.getKey()), "Loaded tracker should be equal to the stored");
        dt.setMatchKeys(Set.of("BLAH")).setStatus(ChunkSchedulingStatus.BLOCKED).setPriority(5);
        trackingStore.store(dt.getKey(), dt);
        assertEquals(dt, trackingStore.load(dt.getKey()), "Store should allow match keys, status and priority to be updated");
    }

    @Test
    void storeAll() {
        initialLoad();
        Map<TrackingKey, DependencyTracking> map = Trackers.getMap();
        trackingStore.deleteAll(map.keySet());
        trackingStore.storeAll(map);
        assertEquals(map, trackingStore.loadAll(map.keySet()), "Loaded trackers be equal to what we stored");
        map.get(Trackers.TRACKER_1_0.key()).setStatus(ChunkSchedulingStatus.BLOCKED).setPriority(1).setWaitingOn(Set.of()).setMatchKeys(Set.of("Hest"));
        trackingStore.storeAll(map);
        assertEquals(map, trackingStore.loadAll(map.keySet()), "Existing entries should be updated");
    }

    void initialLoad() {
        JPATestUtils.runSqlFromResource(emf.createEntityManager(),this, "JobSchedulerBeanIT_findWaitForChunks.sql");
    }

    private enum Trackers {
        TRACKER_1_0(() -> new DependencyTracking(new TrackingKey(1, 0), 0, 123456)
                .setStatus(ChunkSchedulingStatus.QUEUED_FOR_PROCESSING)
                .setPriority(4)
                .setWaitingOn(Set.of(new TrackingKey(3, 0)))
                .setMatchKeys(Set.of("KK2", "K0", "C0"))),
        TRACKERS_2_1(() -> new DependencyTracking(new TrackingKey(2, 1), 0, 0));

        public static Map<TrackingKey, DependencyTracking> getMap() {
            return Arrays.stream(values()).collect(Collectors.toMap(Trackers::key, Trackers::get));
        }

        Supplier<DependencyTracking> supplier;

        Trackers(Supplier<DependencyTracking> supplier) {
            this.supplier = supplier;
        }

        public TrackingKey key() {
            return supplier.get().getKey();
        }

        public DependencyTracking get() {
            return supplier.get();
        }
    }
}
