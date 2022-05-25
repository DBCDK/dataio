package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsConfigurationBeanIT extends IntegrationTest {
    private final EntityManagerFactory entityManagerFactory =
            mock(EntityManagerFactory.class);
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean =
            mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector =
            mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
            mock(JobStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector =
            mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() {
        when(entityManagerFactory.createEntityManager())
                .thenReturn(env().getEntityManager());
        when(flowStoreServiceConnectorBean.getConnector())
                .thenReturn(flowStoreServiceConnector);
        when(jobStoreServiceConnectorBean.getConnector())
                .thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void getDelivery_throwsOnFailureToResolveJob() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(0).build();
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.emptyList());

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(() -> periodicJobsConfigurationBean.getDelivery(chunk), isThrowing(SinkException.class));
    }

    @Test
    public void getDelivery_throwsOnFailureToResolveHarvesterConfig()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId((int) chunk.getJobId())
                .withSpecification(
                        new JobSpecification()
                                .withAncestry(new JobSpecification.Ancestry()
                                        .withHarvesterToken("periodic-jobs:1:2")));

        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));
        when(flowStoreServiceConnector.getHarvesterConfig(1, PeriodicJobsHarvesterConfig.class))
                .thenThrow(new FlowStoreServiceConnectorException("DIED"));

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(() -> periodicJobsConfigurationBean.getDelivery(chunk), isThrowing(SinkException.class));
    }

    @Test
    public void getDelivery_onlyFirstChunkPersists()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException, SQLException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(1)
                .setChunkId(1)
                .build();
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId((int) chunk.getJobId())
                .withSpecification(
                        new JobSpecification()
                                .withAncestry(new JobSpecification.Ancestry()
                                        .withHarvesterToken("periodic-jobs:1:2")));
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());

        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));
        when(flowStoreServiceConnector.getHarvesterConfig(1, PeriodicJobsHarvesterConfig.class))
                .thenReturn(periodicJobsHarvesterConfig);

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        PeriodicJobsDelivery delivery = env().getPersistenceContext().run(() ->
                periodicJobsConfigurationBean.getDelivery(chunk));

        assertThat("delivery.jobId", delivery.getJobId(), is((int) chunk.getJobId()));
        assertThat("delivery.config", delivery.getConfig(), is(periodicJobsHarvesterConfig));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.containsKey((int) chunk.getJobId()), is(true));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(0));
        }
    }

    @Test
    public void getDelivery_firstChunkPersists()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException, SQLException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(1)
                .setChunkId(0)
                .build();
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId((int) chunk.getJobId())
                .withSpecification(
                        new JobSpecification()
                                .withAncestry(new JobSpecification.Ancestry()
                                        .withHarvesterToken("periodic-jobs:1:2")));
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());

        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));
        when(flowStoreServiceConnector.getHarvesterConfig(1, PeriodicJobsHarvesterConfig.class))
                .thenReturn(periodicJobsHarvesterConfig);

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        PeriodicJobsDelivery delivery = env().getPersistenceContext().run(() ->
                periodicJobsConfigurationBean.getDelivery(chunk));

        assertThat("delivery.jobId", delivery.getJobId(), is((int) chunk.getJobId()));
        assertThat("delivery.config", delivery.getConfig(), is(periodicJobsHarvesterConfig));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.containsKey((int) chunk.getJobId()), is(true));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(1));
        }
    }

    @Test
    public void getDelivery_servesFromCache() throws SinkException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(42)
                .setChunkId(5)
                .build();
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());
        final PeriodicJobsDelivery expectedDelivery = new PeriodicJobsDelivery((int) chunk.getJobId());
        expectedDelivery.setConfig(periodicJobsHarvesterConfig);

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        periodicJobsConfigurationBean.deliveryCache.put((int) chunk.getJobId(), expectedDelivery);
        assertThat(periodicJobsConfigurationBean.getDelivery(chunk), is(expectedDelivery));
    }

    @Test
    public void getDelivery_servesFromDatabase() throws SinkException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(42)
                .setChunkId(5)
                .build();
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());
        final PeriodicJobsDelivery expectedDelivery = new PeriodicJobsDelivery((int) chunk.getJobId());
        expectedDelivery.setConfig(periodicJobsHarvesterConfig);

        env().getPersistenceContext().run(() ->
                env().getEntityManager().persist(expectedDelivery));

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(periodicJobsConfigurationBean.getDelivery(chunk), is(expectedDelivery));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.containsKey((int) chunk.getJobId()), is(true));
    }

    private PeriodicJobsConfigurationBean newPeriodicJobsConfigurationBean() {
        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = new PeriodicJobsConfigurationBean();
        periodicJobsConfigurationBean.entityManagerFactory = entityManagerFactory;
        periodicJobsConfigurationBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        periodicJobsConfigurationBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return periodicJobsConfigurationBean;
    }
}
