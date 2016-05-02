package dk.dbc.dataio.jobstore.service.ejb;

import static dk.dbc.commons.testutil.Assert.*;
import dk.dbc.dataio.commons.types.Chunk;
import static dk.dbc.dataio.commons.types.Chunk.Type.PARTITIONED;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import static org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver;
import static org.jboss.shrinkwrap.resolver.impl.maven.task.LoadPomTask.loadPomFromFile;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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