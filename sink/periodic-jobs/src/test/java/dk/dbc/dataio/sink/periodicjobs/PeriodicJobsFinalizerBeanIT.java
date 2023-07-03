package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsFinalizerBeanIT extends IntegrationTest {
    private final PeriodicJobsConfigurationBean periodicJobsConfigurationBean =
            mock(PeriodicJobsConfigurationBean.class);
    private final PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean =
            mock(PeriodicJobsHttpFinalizerBean.class);

    @Test
    public void deletesDataBlocks() throws InvalidMessageException, SQLException {
        final int jobId = 42;
        final PeriodicJobsDataBlock block0 = new PeriodicJobsDataBlock();
        block0.setKey(new PeriodicJobsDataBlock.Key(jobId, 0, 0));
        block0.setSortkey("000000000");
        block0.setBytes(StringUtil.asBytes("0"));
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setKey(new PeriodicJobsDataBlock.Key(jobId, 1, 0));
        block1.setSortkey("000000001");
        block1.setBytes(StringUtil.asBytes("1"));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setKey(new PeriodicJobsDataBlock.Key(jobId, 2, 0));
        block2.setSortkey("000000002");
        block2.setBytes(StringUtil.asBytes("2"));
        final PeriodicJobsDataBlock block3 = new PeriodicJobsDataBlock();
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

        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));
        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        when(periodicJobsConfigurationBean.getDelivery(chunk))
                .thenReturn(delivery);

        final PeriodicJobsFinalizerBean periodicJobsFinalizerBean = newPeriodicJobsFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFinalizerBean.handleTerminationChunk(chunk));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of remaining persisted data blocks",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM datablock"), is(1));
        }

        final PeriodicJobsDataBlock remainingBlock = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDataBlock.class,
                        new PeriodicJobsDataBlock.Key(jobId + 1, 0, 0)));
        assertThat("remaining data block", remainingBlock, is(block3));
    }

    @Test
    public void deletesDelivery() throws InvalidMessageException, SQLException {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery1 = new PeriodicJobsDelivery(jobId);
        delivery1.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));
        final PeriodicJobsDelivery delivery2 = new PeriodicJobsDelivery(jobId + 1);
        // Used to verify that delete only targets specific job ID
        delivery2.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(delivery2);
            env().getEntityManager().persist(delivery1);
        });

        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        when(periodicJobsConfigurationBean.getDelivery(chunk))
                .thenReturn(delivery1);

        final PeriodicJobsFinalizerBean periodicJobsFinalizerBean = newPeriodicJobsFinalizerBean();
        env().getPersistenceContext().run(() ->
                periodicJobsFinalizerBean.handleTerminationChunk(chunk));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of remaining persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(1));
        }

        final PeriodicJobsDelivery remainingDelivery = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDelivery.class, jobId + 1));
        assertThat("remaining delivery", remainingDelivery, is(delivery2));
    }

    @Test
    public void returnsResultOfDelivery() throws InvalidMessageException {
        final int jobId = 42;
        final PeriodicJobsDelivery delivery = new PeriodicJobsDelivery(jobId);
        delivery.setConfig(new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withPickup(new HttpPickup())));

        final Chunk chunk = new Chunk(jobId, 3, Chunk.Type.PROCESSED);
        when(periodicJobsConfigurationBean.getDelivery(chunk))
                .thenReturn(delivery);

        final Chunk expectedResult = new Chunk(jobId, 3, Chunk.Type.DELIVERED);
        when(periodicJobsHttpFinalizerBean.deliver(chunk, delivery))
                .thenReturn(expectedResult);

        final PeriodicJobsFinalizerBean periodicJobsFinalizerBean = newPeriodicJobsFinalizerBean();
        final Chunk result = env().getPersistenceContext().run(() ->
                periodicJobsFinalizerBean.handleTerminationChunk(chunk));

        assertThat("result chunk", result, is(sameInstance(expectedResult)));
    }

    private PeriodicJobsFinalizerBean newPeriodicJobsFinalizerBean() {
        final PeriodicJobsFinalizerBean periodicJobsFinalizerBean = new PeriodicJobsFinalizerBean();
        periodicJobsFinalizerBean.entityManager = env().getEntityManager();
        periodicJobsFinalizerBean.periodicJobsConfigurationBean = periodicJobsConfigurationBean;
        periodicJobsFinalizerBean.periodicJobsHttpFinalizerBean = periodicJobsHttpFinalizerBean;
        return periodicJobsFinalizerBean;
    }
}
