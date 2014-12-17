package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PgJobStoreTest {
    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    final EntityManager entityManager = mock(EntityManager.class);

    @Test
    public void cacheFlow_flowArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.cacheFlow(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JobStoreException {
        final FlowCacheEntity expectedFlowCacheEntity = new FlowCacheEntity();
        final Query storeProcedure = mock(Query.class);
        when(entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(storeProcedure);
        when(storeProcedure.getSingleResult()).thenReturn(expectedFlowCacheEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(new FlowBuilder().build());

        assertThat(flowCacheEntity, is(expectedFlowCacheEntity));
    }

    @Test
    public void cacheSink_sinkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.cacheSink(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JobStoreException {
        final SinkCacheEntity expectedSinkCacheEntity = new SinkCacheEntity();
        final Query storeProcedure = mock(Query.class);
        when(entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(storeProcedure);
        when(storeProcedure.getSingleResult()).thenReturn(expectedSinkCacheEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(new SinkBuilder().build());

        assertThat(sinkCacheEntity, is(expectedSinkCacheEntity));
    }

    @Test
    public void addJob_jobArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addJob(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addJob_jobArgIsValid_jobIsPersistedAndManagedEntityIsRefreshed() {
        final JobEntity job = new JobEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = pgJobStore.addJob(job);

        assertThat(jobEntity, is(job));
        verify(entityManager).persist(job);
        verify(entityManager).refresh(job);
    }

    @Test
    public void addChunk_chunkArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addChunk_chunkArgIsValid_chunkIsPersistedAndManagedEntityIsRefreshed() {
        final ChunkEntity chunk = new ChunkEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final ChunkEntity chunkEntity = pgJobStore.addChunk(chunk);

        assertThat(chunkEntity, is(chunk));
        verify(entityManager).persist(chunk);
        verify(entityManager).refresh(chunk);
    }

    /*
     * Private methods
     */

    private PgJobStore newPgJobStore() {
        final JSONBBean jsonbBean = new JSONBBean();
        jsonbBean.initialiseContext();

        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.jsonbBean = jsonbBean;
        pgJobStore.entityManager = entityManager;
        return pgJobStore;
    }
}