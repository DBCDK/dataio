package dk.dbc.dataio.jobstore.service.ejb;


import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import org.junit.Test;

import java.util.Collections;

import static dk.dbc.dataio.jobstore.types.State.Phase.PARTITIONING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStore_ResourceBundleTest extends PgJobStoreBaseTest {


    @Test
    public void getResourceBundle_jobEntityNotFound_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);
        try {
            pgJobStore.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void getResourceBundle_flowIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Sink sink = new SinkBuilder().build();

        FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID, Collections.singletonList(PARTITIONING));
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(null);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        try {
            pgJobStore.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void getResourceBundle_sinkIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();

        FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID, Collections.singletonList(PARTITIONING));
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(flow);
        when(jobEntity.getCachedSink().getSink()).thenReturn(null);

        try {
            pgJobStore.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void getResourceBundle_resourcesAddedToBundle_returns() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        JobEntity jobEntity = getJobEntity(1, Collections.singletonList(PARTITIONING));
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(flow);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        final ResourceBundle resourceBundle = pgJobStore.getResourceBundle(jobEntity.getId());
        assertThat("ResourceBundle not null", resourceBundle, not(nullValue()));
        assertThat(String.format("ResourceBundle.flow: %s expected to match: %s", resourceBundle.getFlow(), flow), resourceBundle.getFlow(), is(flow));
        assertThat(String.format("ResourceBundle.sink: %s expected to match: %s", resourceBundle.getSink(), sink), resourceBundle.getSink(), is(sink));

        assertThat(String.format("ResourceBundle.supplementaryProcessData.format: %s expected to match: %s:",
                        resourceBundle.getSupplementaryProcessData().getFormat(),
                        jobEntity.getSpecification().getFormat()),
                resourceBundle.getSupplementaryProcessData().getFormat(), is(jobEntity.getSpecification().getFormat()));

        assertThat(String.format("ResourceBundle.supplementaryProcessData.submitter: %s expected to match: %s:",
                        resourceBundle.getSupplementaryProcessData().getSubmitter(),
                        jobEntity.getSpecification().getSubmitterId()),
                resourceBundle.getSupplementaryProcessData().getSubmitter(), is(jobEntity.getSpecification().getSubmitterId()));
    }

}