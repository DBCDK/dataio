package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
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


}