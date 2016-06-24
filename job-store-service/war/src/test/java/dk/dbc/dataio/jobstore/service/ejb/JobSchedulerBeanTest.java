package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Created by ja7 on 11-04-16.
 *
 */
public class JobSchedulerBeanTest {

    @Test
    public void scheduleChunkThowsOnNullInpub()  {
        JobSchedulerBean bean=new JobSchedulerBean();

        assertThat( () -> { bean.scheduleChunk( null, new SinkBuilder().build());}, isThrowing(NullPointerException.class));
        assertThat( () -> { bean.scheduleChunk( new ChunkEntity() , null );}, isThrowing(NullPointerException.class));
    }

    @Test
    public void queuedToSinkCounter() throws Exception {

        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(1));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(2));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(3));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(2L), is(1));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(4));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(2L), is(2));
        assertThat(JobSchedulerBean.decrementAndReturnCurrentQueuedToDelivering(1L), is(3));
        assertThat(JobSchedulerBean.decrementAndReturnCurrentQueuedToDelivering(3L), is(-1));
    }


    @Test
    public void queuedToProcessingCounter() throws Exception {

        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(1));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(2));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(3));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(2), is(1));
        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(4));

        assertThat(JobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(2), is(2));
        assertThat(JobSchedulerBean.decrementAndReturnCurrentQueuedToProcessing(1), is(3));
        assertThat(JobSchedulerBean.decrementAndReturnCurrentQueuedToProcessing(3), is(-1));
    }

}