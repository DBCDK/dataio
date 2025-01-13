package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.core.JetTestSupport;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

public class DependencyTrackingServicePerformanceTest extends JetTestSupport {
    HazelcastInstance hz;

    @Test @Ignore
    public void testPerformance() throws IOException {
        startHazelcast();
        DependencyTrackingService service = new DependencyTrackingService();
        Set<String> matchKeys = new HashSet<>();
        Random random = new Random();
        IntStream.range(0, 10000).forEach(i -> {
            TrackingKey key = new TrackingKey(1, i);
            if(i % 1000 == 0) matchKeys.add(Integer.toHexString(random.nextInt()));
            DependencyTracking dependencyTracking = new DependencyTracking(key, 0, 0, matchKeys);
            service.addAndBuildDependencies(dependencyTracking, null);
            if(i % 1000 == 0) System.out.println("Added " + i + " dependencies, current match keys " + matchKeys);
        });

    }

    private void startHazelcast() throws IOException {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("hz-data.xml")) {
            Config config = Hazelcast.makeConfig(is);
            config.getMapConfig("dependencies").getMapStoreConfig().setEnabled(false);
            hz = createHazelcastInstance(config);
            Hazelcast.testInstance(hz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
