package dk.dbc.dataio.jobstore.service.ejb;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScheduledWatermarkPurgeBeanTest {
    private final WatermarkPurgeBean watermarkPurgeBean = mock(WatermarkPurgeBean.class);

    /* run() guards on Hazelcast.isSlave(), which dereferences the static
       Hazelcast.INSTANCE. Without this, INSTANCE is null in a plain unit test,
       isSlave() throws NPE, and purgeStaleWatermarks() is never reached. */
    @BeforeEach
    public void makeThisNodeMaster() {
        final Member localMember = mock(Member.class);
        when(localMember.localMember()).thenReturn(true);
        final Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(Set.of(localMember));
        final HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
        when(hazelcastInstance.getCluster()).thenReturn(cluster);
        Hazelcast.testInstance(hazelcastInstance);
    }

    @AfterEach
    public void resetHazelcastInstance() {
        Hazelcast.testInstance(null);
    }

    @Test
    public void run_delegatesToWatermarkPurgeBean() {
        createScheduledWatermarkPurgeBean().run();

        verify(watermarkPurgeBean).purgeStaleWatermarks();
    }

    @Test
    public void run_watermarkPurgeBeanThrowsUncheckedException_noExceptionThrown() {
        final ScheduledWatermarkPurgeBean bean = createScheduledWatermarkPurgeBean();
        doThrow(new RuntimeException("DIED")).when(watermarkPurgeBean).purgeStaleWatermarks();

        bean.run();

        verify(watermarkPurgeBean).purgeStaleWatermarks();
    }

    private ScheduledWatermarkPurgeBean createScheduledWatermarkPurgeBean() {
        final ScheduledWatermarkPurgeBean bean = new ScheduledWatermarkPurgeBean();
        bean.watermarkPurgeBean = watermarkPurgeBean;
        return bean;
    }
}
