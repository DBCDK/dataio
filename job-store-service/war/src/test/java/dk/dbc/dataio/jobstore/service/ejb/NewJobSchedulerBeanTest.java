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
public class NewJobSchedulerBeanTest {

    @Test
    public void scheduleChunkThowsOnNullInpub()  {
        NewJobSchedulerBean bean=new NewJobSchedulerBean();

        assertThat( () -> { bean.scheduleChunk( null, new SinkBuilder().build());}, isThrowing(NullPointerException.class));
        assertThat( () -> { bean.scheduleChunk( new ChunkEntity() , null );}, isThrowing(NullPointerException.class));
    }

    @Test
    public void queuedToSinkCounter() throws Exception {

        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(1));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(2));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(3));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(2L), is(1));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(1L), is(4));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToDelivering(2L), is(2));
        assertThat(NewJobSchedulerBean.decrementAndReturnCurrentQueuedToDelivering(1L), is(3));
        assertThat(NewJobSchedulerBean.decrementAndReturnCurrentQueuedToDelivering(3L), is(-1));
    }


    @Test
    public void queuedToProcessingCounter() throws Exception {

        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(1));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(2));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(3));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(2), is(1));
        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(1), is(4));

        assertThat(NewJobSchedulerBean.incrementAndReturnCurrentQueuedToProcessing(2), is(2));
        assertThat(NewJobSchedulerBean.decrementAndReturnCurrentQueuedToProcessing(1), is(3));
        assertThat(NewJobSchedulerBean.decrementAndReturnCurrentQueuedToProcessing(3), is(-1));
    }

}