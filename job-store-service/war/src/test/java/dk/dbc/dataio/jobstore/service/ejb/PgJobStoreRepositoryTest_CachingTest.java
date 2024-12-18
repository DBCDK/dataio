package dk.dbc.dataio.jobstore.service.ejb;


import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PgJobStoreRepositoryTest_CachingTest extends PgJobStoreBaseTest {
    @org.junit.Test
    public void cacheFlow_flowArgIsNull_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(NullPointerException.class, () -> pgJobStoreRepository.cacheFlow(null));
    }

    @org.junit.Test
    public void cacheFlow_flowArgIsEmpty_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(IllegalArgumentException.class, () -> pgJobStoreRepository.cacheFlow(" "));
    }

    @org.junit.Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JSONBException {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        String flowJson = pgJobStoreRepository.jsonbContext.marshall(new FlowBuilder().build());
        FlowCacheEntity flowCacheEntity = pgJobStoreRepository.cacheFlow(flowJson);
        assertThat(flowCacheEntity, is(EXPECTED_FLOW_CACHE_ENTITY));
    }

    @org.junit.Test
    public void cacheSink_sinkArgIsNull_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(NullPointerException.class, () -> pgJobStoreRepository.cacheSink(null));
    }

    @org.junit.Test
    public void cacheSink_sinkArgIsEmpty_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(IllegalArgumentException.class, () -> pgJobStoreRepository.cacheSink(" "));
    }

    @org.junit.Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JSONBException {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        String sinkJson = pgJobStoreRepository.jsonbContext.marshall(new SinkBuilder().build());
        SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(sinkJson);
        assertThat(sinkCacheEntity, is(EXPECTED_SINK_CACHE_ENTITY));
    }
}
