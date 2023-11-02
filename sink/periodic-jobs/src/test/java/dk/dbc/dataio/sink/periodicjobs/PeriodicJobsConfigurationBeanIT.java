package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodicJobsConfigurationBeanIT extends IntegrationTest {
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
        assertThat(() -> periodicJobsConfigurationBean.getDelivery(chunk, env().getEntityManager()), isThrowing(RuntimeException.class));
    }

    @Test
    public void getDelivery_throwsOnFailureToResolveHarvesterConfig()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId(chunk.getJobId())
                .withSpecification(
                        new JobSpecification()
                                .withAncestry(new JobSpecification.Ancestry()
                                        .withHarvesterToken("periodic-jobs:1:2")));

        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));
        when(flowStoreServiceConnector.getHarvesterConfig(1, PeriodicJobsHarvesterConfig.class))
                .thenThrow(new FlowStoreServiceConnectorException("DIED"));

        PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(() -> periodicJobsConfigurationBean.getDelivery(chunk, env().getEntityManager()), isThrowing(RuntimeException.class));
    }

    @Test
    public void getDelivery_onlyFirstChunkPersists()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException, SQLException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(1)
                .setChunkId(1)
                .build();
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId(chunk.getJobId())
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
                periodicJobsConfigurationBean.getDelivery(chunk, env().getEntityManager()));

        assertThat("delivery.jobId", delivery.getJobId(), is(chunk.getJobId()));
        assertThat("delivery.config", delivery.getConfig(), is(periodicJobsHarvesterConfig));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.getIfPresent(chunk.getJobId()), is(notNullValue()));

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
                .withJobId(chunk.getJobId())
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
                periodicJobsConfigurationBean.getDelivery(chunk, env().getEntityManager()));

        assertThat("delivery.jobId", delivery.getJobId(), is(chunk.getJobId()));
        assertThat("delivery.config", delivery.getConfig(), is(periodicJobsHarvesterConfig));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.getIfPresent(chunk.getJobId()), is(notNullValue()));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(1));
        }
    }

    @Test
    public void getDelivery_servesFromCache() throws InvalidMessageException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(42)
                .setChunkId(5)
                .build();
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());
        final PeriodicJobsDelivery expectedDelivery = new PeriodicJobsDelivery(chunk.getJobId());
        expectedDelivery.setConfig(periodicJobsHarvesterConfig);

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        periodicJobsConfigurationBean.deliveryCache.put(chunk.getJobId(), expectedDelivery);
        assertThat(periodicJobsConfigurationBean.getDelivery(chunk, env().getEntityManager()), is(expectedDelivery));
    }

    @Test
    public void getDelivery_servesFromDatabase() throws InvalidMessageException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(42)
                .setChunkId(5)
                .build();
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());
        final PeriodicJobsDelivery expectedDelivery = new PeriodicJobsDelivery(chunk.getJobId());
        expectedDelivery.setConfig(periodicJobsHarvesterConfig);

        env().getPersistenceContext().run(() ->
                env().getEntityManager().persist(expectedDelivery));

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(periodicJobsConfigurationBean.getDelivery(chunk, env().getEntityManager()), is(expectedDelivery));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.getIfPresent(chunk.getJobId()), is(notNullValue()));
    }

    private PeriodicJobsConfigurationBean newPeriodicJobsConfigurationBean() {
        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = new PeriodicJobsConfigurationBean();
        periodicJobsConfigurationBean.flowStoreServiceConnector = flowStoreServiceConnector;
        periodicJobsConfigurationBean.jobStoreServiceConnector = jobStoreServiceConnector;
        return periodicJobsConfigurationBean;
    }
}
