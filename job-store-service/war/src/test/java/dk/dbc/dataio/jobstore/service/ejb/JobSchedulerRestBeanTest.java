package dk.dbc.dataio.jobstore.service.ejb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Created by ja7 on 19-07-16.
 */
public class JobSchedulerRestBeanTest {
    @Test
    public void forceBulkMode() throws Exception {
        JobSchedulerRestBean jobSchedulerRestBean=new JobSchedulerRestBean();
        int sinkId=3;
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(true));
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(true));
        jobSchedulerRestBean.forceSinkIntoBulkMode("{\"sink\":3}", 3);
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(false));
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(false));
    }


    @Test
    public void forceTransitionModeMode() throws Exception {
        JobSchedulerRestBean jobSchedulerRestBean=new JobSchedulerRestBean();
        int sinkId=3;
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(true));
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(true));
        jobSchedulerRestBean.forceSinkIntoTransitionToDirectMode("{\"sink\":3}", 3);
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isProcessingModeDirectSubmit(), is(true));
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).isDeliveringModeDirectSubmit(), is(true));

        assertThat( JobSchedulerBean.getSinkStatus(sinkId).processingStatus.getMode(), is(JobSchedulerBean.QueueSubmitMode.TRANSITION_TO_DIRECT));
        assertThat( JobSchedulerBean.getSinkStatus(sinkId).deliveringStatus.getMode(), is(JobSchedulerBean.QueueSubmitMode.TRANSITION_TO_DIRECT));
    }

}