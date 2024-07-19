package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import org.junit.Test;

import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PgJobStoreRepositoryIT_CachingIT extends PgJobStoreRepositoryAbstractIT {

    /**
     * Given: a job store with empty flowcache
     * When : a flow is added
     * Then : the flow is inserted into the flowcache
     */
    @Test
    public void cacheFlow_addingNeverBeforeSeenFlow_isCached() throws JSONBException, SQLException {
        // Given...
        final Flow flow = new FlowBuilder().build();

        // When...
        final FlowCacheEntity flowCacheEntity = pgJobStoreRepository.cacheFlow(
                pgJobStoreRepository.jsonbContext.marshall(flow));

        // Then...
        assertThat("entity", flowCacheEntity, is(notNullValue()));
        assertThat("table size", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
        assertThat("entity.flow.id", flowCacheEntity.getFlow().getId(), is(flow.getId()));
    }

    /**
     * Given: a job store with non-empty flowcache
     * When : a flow is added matching an already cached flow
     * Then : no new flow is inserted into the flowcache
     */
    @Test
    public void cacheFlow_addingAlreadyCachedFlow_leavesCacheUnchanged() throws JSONBException, SQLException {
        // Given...
        final FlowCacheEntity existingFlowCacheEntity = newPersistedFlowCacheEntity();

        // When...
        final FlowCacheEntity flowCacheEntity = pgJobStoreRepository.cacheFlow(
                pgJobStoreRepository.jsonbContext.marshall(existingFlowCacheEntity.getFlow()));

        // Then...
        assertThat("entity.id", flowCacheEntity.getId(), is(existingFlowCacheEntity.getId()));
        assertThat("entity.checksum", flowCacheEntity.getChecksum(), is(existingFlowCacheEntity.getChecksum()));
        assertThat("entity.flow", flowCacheEntity.getFlow(), is(existingFlowCacheEntity.getFlow()));
        assertThat("table size", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
    }

    /**
     * Given: a job store with empty sinkcache
     * When : a sink is added
     * Then : the sink is inserted into the sinkcache
     */
    @Test
    public void cacheSink_addingNeverBeforeSeenSink_isCached() throws JSONBException, SQLException {
        // Given...
        final Sink sink = new SinkBuilder().build();

        // When...
        final SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(pgJobStoreRepository.jsonbContext.marshall(sink));

        // Then...
        assertThat("entity", sinkCacheEntity, is(notNullValue()));
        assertThat("table size", getSizeOfTable(SINK_CACHE_TABLE_NAME), is(1L));
        assertThat("entity.sink.id", sinkCacheEntity.getSink().getId(), is(sink.getId()));
    }

    /**
     * Given: a job store with non-empty sinkcache
     * When : a sink is added matching an already cached sink
     * Then : no new sink is inserted into the sinkcache
     */
    @Test
    public void cacheSink_addingAlreadyCachedSink_leavesCacheUnchanged() throws JSONBException, SQLException {
        // Given...
        final SinkCacheEntity existingSinkCacheEntity = newPersistedSinkCacheEntity();

        // When...
        final SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(pgJobStoreRepository.jsonbContext.marshall(existingSinkCacheEntity.getSink()));

        // Then...
        assertThat("entity.id", sinkCacheEntity.getId(), is(existingSinkCacheEntity.getId()));
        assertThat("entity.checksum", sinkCacheEntity.getChecksum(), is(existingSinkCacheEntity.getChecksum()));
        assertThat("entity.sink", sinkCacheEntity.getSink(), is(existingSinkCacheEntity.getSink()));
        assertThat("table size", getSizeOfTable(SINK_CACHE_TABLE_NAME), is(1L));
    }
}
