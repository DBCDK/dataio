package dk.dbc.dataio.sink.periodicjobs.pickup;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.sink.periodicjobs.IntegrationTest;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsConfigurationBean;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDataBlock;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsDelivery;
import dk.dbc.dataio.sink.periodicjobs.PeriodicJobsFinalizerBean;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsFinalizerBeanIT extends IntegrationTest {
    /* Id of the job's termination chunk, non-zero so the job counts as having data */
    private static final int CHUNK_ID = 3;

    private final PeriodicJobsConfigurationBean periodicJobsConfigurationBean =
            mock(PeriodicJobsConfigurationBean.class);
    private final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean =
            mock(PeriodicJobsHttpFinalizerBean.class);

    @Test
    public void deletesDataBlocks() throws SQLException {
        final int jobId = 42;
        PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0"));
        PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1"));
        PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2"));
        PeriodicJobsDataBlock block3 = new PeriodicJobsDataBlock();
        // Used to verify that delete only targets specific job ID
        block3.setKey(new PeriodicJobsDataBlock.Key(jobId + 1, 0, 0));
        block3.setSortkey("000000000");
        block3.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(block3);
            env().getEntityManager().persist(block2);
            env().getEntityManager().persist(block1);
            env().getEntityManager().persist(block0);
        });

        PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));
        when(periodicJobsConfigurationBean.getDelivery(jobId, CHUNK_ID, env().getEntityManager()))
                .thenReturn(delivery);

        PeriodicJobsFinalizerBean periodicJobsFinalizerBean = newPeriodicJobsFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFinalizerBean.finalizeJob(jobId, CHUNK_ID, env().getEntityManager()));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of remaining persisted data blocks",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM datablock"), is(1));
        }

        PeriodicJobsDataBlock remainingBlock = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDataBlock.class,
                        new PeriodicJobsDataBlock.Key(jobId + 1, 0, 0)));
        assertThat("remaining data block", remainingBlock, is(block3));
    }

    @Test
    public void deletesDelivery() throws SQLException {
        final int jobId = 42;
        PeriodicJobsDelivery delivery1 = new PeriodicJobsDelivery(jobId);
        delivery1.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));
        PeriodicJobsDelivery delivery2 = new PeriodicJobsDelivery(jobId + 1);
        // Used to verify that delete only targets specific job ID
        delivery2.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(delivery2);
            env().getEntityManager().persist(delivery1);
        });

        when(periodicJobsConfigurationBean.getDelivery(jobId, CHUNK_ID, env().getEntityManager()))
                .thenReturn(delivery1);

        PeriodicJobsFinalizerBean periodicJobsFinalizerBean = newPeriodicJobsFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFinalizerBean.finalizeJob(jobId, CHUNK_ID, env().getEntityManager()));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of remaining persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(1));
        }

        PeriodicJobsDelivery remainingDelivery = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDelivery.class, jobId + 1));
        assertThat("remaining delivery", remainingDelivery, is(delivery2));
    }

    @Test
    public void returnsResultOfDelivery() throws InvalidMessageException {
        final int jobId = 42;
        PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));

        when(periodicJobsConfigurationBean.getDelivery(jobId, CHUNK_ID, env().getEntityManager()))
                .thenReturn(delivery);

        ChunkItem expectedResult = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.JOB_END);
        when(periodicJobsHttpFinalizerBean.deliver(jobId, CHUNK_ID, delivery, env().getEntityManager()))
                .thenReturn(expectedResult);

        PeriodicJobsFinalizerBean periodicJobsFinalizerBean = newPeriodicJobsFinalizerBean();
        ChunkItem result = env().getPersistenceContext().run(() ->
                periodicJobsFinalizerBean.finalizeJob(jobId, CHUNK_ID, env().getEntityManager()));

        assertThat("result item", result, is(sameInstance(expectedResult)));
    }

    private PeriodicJobsFinalizerBean newPeriodicJobsFinalizerBean() {
        PeriodicJobsFinalizerBean periodicJobsFinalizerBean = new PeriodicJobsFinalizerBean();
        periodicJobsFinalizerBean
                .withPeriodicJobsConfigurationBean(periodicJobsConfigurationBean)
                .withPeriodicJobsHttpFinalizerBean(periodicJobsHttpFinalizerBean);
        return periodicJobsFinalizerBean;
    }
}
