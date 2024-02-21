package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class Hazelcast {
    public static final HazelcastInstance INSTANCE = startInstance();

    private static HazelcastInstance startInstance() {
        String configFile = Optional.ofNullable(System.getenv("JOBSTORE_HZ_CONFIG"))
                .filter(s -> !s.isEmpty())
                .orElse("/opt/payara6/deployments/hz-data.xml");
        try(InputStream is = new FileInputStream(configFile)) {
            Config config = new XmlConfigBuilder(is).build();
            config.setInstanceName("jobstore-" + System.getenv("HOSTNAME"));
            return com.hazelcast.core.Hazelcast.newHazelcastInstance(config);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start hazelcast data instance", e);
        }
    }

    public static boolean isMaster() {
        return INSTANCE.getCluster().getMembers().stream().findFirst().map(Member::localMember).orElse(false);
    }
}
