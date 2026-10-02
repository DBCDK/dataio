package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
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
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.emptyList());

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(() -> periodicJobsConfigurationBean.getDelivery(0, 1, env().getEntityManager()), isThrowing(RuntimeException.class));
    }

    @Test
    public void getDelivery_throwsOnFailureToResolveHarvesterConfig()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final int jobId = 3;
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId(jobId)
                .withSpecification(
                        new JobSpecification()
                                .withAncestry(new JobSpecification.Ancestry()
                                        .withHarvesterToken("periodic-jobs:1:2")));

        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class)))
                .thenReturn(Collections.singletonList(jobInfoSnapshot));
        when(flowStoreServiceConnector.getHarvesterConfig(1, PeriodicJobsHarvesterConfig.class))
                .thenThrow(new FlowStoreServiceConnectorException("DIED"));

        PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(() -> periodicJobsConfigurationBean.getDelivery(jobId, 1, env().getEntityManager()), isThrowing(RuntimeException.class));
    }

    @Test
    public void getDelivery_onlyFirstChunkPersists()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException, SQLException {
        final int jobId = 1;
        final int chunkId = 1;
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId(jobId)
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
                periodicJobsConfigurationBean.getDelivery(jobId, chunkId, env().getEntityManager()));

        assertThat("delivery.jobId", delivery.getJobId(), is(jobId));
        assertThat("delivery.config", delivery.getConfig(), is(periodicJobsHarvesterConfig));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.getIfPresent(jobId), is(notNullValue()));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(0));
        }
    }

    @Test
    public void getDelivery_firstChunkPersists()
            throws JobStoreServiceConnectorException, FlowStoreServiceConnectorException, SQLException {
        final int jobId = 1;
        final int chunkId = 0;
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withJobId(jobId)
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
                periodicJobsConfigurationBean.getDelivery(jobId, chunkId, env().getEntityManager()));

        assertThat("delivery.jobId", delivery.getJobId(), is(jobId));
        assertThat("delivery.config", delivery.getConfig(), is(periodicJobsHarvesterConfig));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.getIfPresent(jobId), is(notNullValue()));

        try (Connection conn = connectToPeriodicJobsDB()) {
            assertThat("number of persisted deliveries",
                    JDBCUtil.getFirstInt(conn, "SELECT COUNT(*) FROM delivery"), is(1));
        }
    }

    @Test
    public void getDelivery_servesFromCache() throws InvalidMessageException {
        final int jobId = 42;
        final int chunkId = 5;
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());
        final PeriodicJobsDelivery expectedDelivery = new PeriodicJobsDelivery(jobId);
        expectedDelivery.setConfig(periodicJobsHarvesterConfig);

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        periodicJobsConfigurationBean.deliveryCache.put(jobId, expectedDelivery);
        assertThat(periodicJobsConfigurationBean.getDelivery(jobId, chunkId, env().getEntityManager()), is(expectedDelivery));
    }

    @Test
    public void getDelivery_servesFromDatabase() throws InvalidMessageException {
        final int jobId = 42;
        final int chunkId = 5;
        final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig =
                new PeriodicJobsHarvesterConfig(1, 1, new PeriodicJobsHarvesterConfig.Content());
        final PeriodicJobsDelivery expectedDelivery = new PeriodicJobsDelivery(jobId);
        expectedDelivery.setConfig(periodicJobsHarvesterConfig);

        env().getPersistenceContext().run(() ->
                env().getEntityManager().persist(expectedDelivery));

        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = newPeriodicJobsConfigurationBean();
        assertThat(periodicJobsConfigurationBean.getDelivery(jobId, chunkId, env().getEntityManager()), is(expectedDelivery));
        assertThat("delivery is cached",
                periodicJobsConfigurationBean.deliveryCache.getIfPresent(jobId), is(notNullValue()));
    }

    private PeriodicJobsConfigurationBean newPeriodicJobsConfigurationBean() {
        final PeriodicJobsConfigurationBean periodicJobsConfigurationBean = new PeriodicJobsConfigurationBean();
        periodicJobsConfigurationBean.flowStoreServiceConnector = flowStoreServiceConnector;
        periodicJobsConfigurationBean.jobStoreServiceConnector = jobStoreServiceConnector;
        return periodicJobsConfigurationBean;
    }
}
