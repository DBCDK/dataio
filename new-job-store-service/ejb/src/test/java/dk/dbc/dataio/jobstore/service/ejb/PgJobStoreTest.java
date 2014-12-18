package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
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
    public void persistJob_jobArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.persistJob(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void persistJob_jobArgIsValid_jobIsPersistedAndManagedEntityIsRefreshed() {
        final JobEntity job = new JobEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = pgJobStore.persistJob(job);

        assertThat(jobEntity, is(job));
        verify(entityManager).persist(job);
        verify(entityManager).refresh(job);
    }

    @Test
    public void persistChunk_chunkArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.persistChunk(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void persistChunk_chunkArgIsValid_chunkIsPersistedAndManagedEntityIsRefreshed() {
        final ChunkEntity chunk = new ChunkEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final ChunkEntity chunkEntity = pgJobStore.persistChunk(chunk);

        assertThat(chunkEntity, is(chunk));
        verify(entityManager).persist(chunk);
        verify(entityManager).refresh(chunk);
    }

    @Test
    public void persistItem_itemArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.persistItem(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void persistItem_itemArgIsValid_itemIsPersistedAndManagedEntityIsRefreshed() {
        final ItemEntity item = new ItemEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final ItemEntity itemEntity = pgJobStore.persistItem(item);

        assertThat(itemEntity, is(item));
        verify(entityManager).persist(item);
        verify(entityManager).refresh(item);
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