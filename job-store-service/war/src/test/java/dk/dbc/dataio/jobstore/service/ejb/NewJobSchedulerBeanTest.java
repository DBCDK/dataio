package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.commons.testutil.Assert.*;
import dk.dbc.dataio.commons.types.Chunk;
import static dk.dbc.dataio.commons.types.Chunk.Type.PARTITIONED;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import static org.junit.Assert.*;
import org.junit.Test;

import java.net.InetAddress;

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



}