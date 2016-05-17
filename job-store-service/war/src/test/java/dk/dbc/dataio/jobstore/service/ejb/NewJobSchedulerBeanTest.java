package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Created by ja7 on 11-04-16.
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
        NewJobSchedulerBean bean=new NewJobSchedulerBean();

        Sink sink1=new SinkBuilder().setId(1).build();
        Sink sink2=new SinkBuilder().setId(2).build();
        Sink sink3=new SinkBuilder().setId(3).build();

        assertThat(bean.incrementAndReturnCurrentQueuedToDelivering(sink1), is(1));
        assertThat(bean.incrementAndReturnCurrentQueuedToDelivering(sink1), is(2));
        assertThat(bean.incrementAndReturnCurrentQueuedToDelivering(sink1), is(3));
        assertThat(bean.incrementAndReturnCurrentQueuedToDelivering(sink2), is(1));
        assertThat(bean.incrementAndReturnCurrentQueuedToDelivering(sink1), is(4));

        assertThat(bean.incrementAndReturnCurrentQueuedToDelivering(sink2), is(2));
        assertThat(bean.decrementAndReturnCurrentQueuedToDelivering(sink1), is(3));
        assertThat(bean.decrementAndReturnCurrentQueuedToDelivering(sink3), is(-1));
    }


    @Test
    public void queuedToProcessingCounter() throws Exception {
        NewJobSchedulerBean bean=new NewJobSchedulerBean();

        assertThat(bean.incrementAndReturnCurrentQueuedToProcessing(1), is(1));
        assertThat(bean.incrementAndReturnCurrentQueuedToProcessing(1), is(2));
        assertThat(bean.incrementAndReturnCurrentQueuedToProcessing(1), is(3));
        assertThat(bean.incrementAndReturnCurrentQueuedToProcessing(2), is(1));
        assertThat(bean.incrementAndReturnCurrentQueuedToProcessing(1), is(4));

        assertThat(bean.incrementAndReturnCurrentQueuedToProcessing(2), is(2));
        assertThat(bean.decrementAndReturnCurrentQueuedToProcessing(1), is(3));
        assertThat(bean.decrementAndReturnCurrentQueuedToProcessing(3), is(-1));
    }

}