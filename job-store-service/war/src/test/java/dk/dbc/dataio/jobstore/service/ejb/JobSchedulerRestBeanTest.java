package dk.dbc.dataio.jobstore.service.ejb;

import com.hazelcast.jet.core.JetTestSupport;
import dk.dbc.dataio.jobstore.distributed.QueueSubmitMode;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by ja7 on 19-07-16.
 */
public class JobSchedulerRestBeanTest extends JetTestSupport {
    @Test
    public void forceBulkMode() throws Exception {
        Hazelcast.testInstance(createHazelcastInstance());
        DependencyTrackingService service = new DependencyTrackingService().init();
        JobSchedulerRestBean jobSchedulerRestBean = new JobSchedulerRestBean(service);
        int sinkId = 3;
        assertThat(service.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(true));
        assertThat(service.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(true));
        jobSchedulerRestBean.forceSinkIntoBulkMode("{\"sink\":3}", 3);
        assertThat(service.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(false));
        assertThat(service.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(false));
    }


    @Test
    public void forceTransitionModeMode() throws Exception {
        Hazelcast.testInstance(createHazelcastInstance());
        DependencyTrackingService service = new DependencyTrackingService().init();
        int sinkId = 3;
        assertThat(service.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(true));
        assertThat(service.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(true));
        new JobSchedulerRestBean(service).forceSinkIntoTransitionToDirectMode("{\"sink\":3}", 3);
        assertThat(service.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(true));
        assertThat(service.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(true));

        assertThat(service.getSinkStatus(sinkId).getProcessingStatus().getMode(), is(QueueSubmitMode.TRANSITION_TO_DIRECT));
        assertThat(service.getSinkStatus(sinkId).getDeliveringStatus().getMode(), is(QueueSubmitMode.TRANSITION_TO_DIRECT));
    }

}
