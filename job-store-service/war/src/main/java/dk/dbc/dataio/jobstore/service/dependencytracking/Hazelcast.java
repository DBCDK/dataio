package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

public class Hazelcast {
    private static HazelcastInstance INSTANCE;

    private static HazelcastInstance startInstance() {
        String configFile = Optional.ofNullable(System.getenv("JOBSTORE_HZ_CONFIG"))
                .filter(s -> !s.isEmpty())
                .orElse("/opt/payara6/deployments/hz-data.xml");
        try(InputStream is = new FileInputStream(configFile)) {
            Config config = new XmlConfigBuilder(is).build();
            config.setInstanceName(System.getenv("HOSTNAME"));
            config.setClassLoader(Hazelcast.class.getClassLoader());
            return com.hazelcast.core.Hazelcast.newHazelcastInstance(config);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start hazelcast data instance", e);
        }
    }

    private static HazelcastInstance getInstance() {
        if(INSTANCE != null) return INSTANCE;
        synchronized (Hazelcast.class) {
            if(INSTANCE == null) INSTANCE = startInstance();
        }
        return INSTANCE;
    }

    public static void testInstance(HazelcastInstance instance) {
        INSTANCE = instance;
    }

    public static boolean isMaster() {
        return INSTANCE.getCluster().getMembers().stream().findFirst().map(Member::localMember).orElse(false);
    }

    public static boolean isReady() {
        return Hazelcast.getInstance().getLifecycleService().isRunning() && Hazelcast.getInstance().getPartitionService().isClusterSafe();
    }

    public enum Objects {
        DEPENDENCY_TRACKING(() -> getInstance().getMap("dependencies")),
        ABORTED_JOBS(() -> getInstance().getSet("aborted.jobs"));

        final Supplier<?> supplier;

        Objects(Supplier<?> supplier) {
            this.supplier = supplier;
        }

        @SuppressWarnings("unchecked")
        public <T> T get() {
            return (T)supplier.get();
        }
    }
}
