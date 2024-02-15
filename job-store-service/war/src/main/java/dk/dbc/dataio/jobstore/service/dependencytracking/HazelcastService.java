package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HazelcastService {
    @Inject
    private HazelcastInstance instance;

    public boolean isMaster() {
        return instance.getCluster().getMembers().stream().findFirst().map(Member::localMember).orElse(false);
    }

    public HazelcastInstance getInstance() {
        return instance;
    }
}
