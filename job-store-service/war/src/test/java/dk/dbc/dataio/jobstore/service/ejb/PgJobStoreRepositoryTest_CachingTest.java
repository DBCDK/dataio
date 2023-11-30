package dk.dbc.dataio.jobstore.service.ejb;


import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PgJobStoreRepositoryTest_CachingTest extends PgJobStoreBaseTest {
    @Test
    public void cacheFlow_flowArgIsNull_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(NullPointerException.class, () -> pgJobStoreRepository.cacheFlow(null));
    }

    @Test
    public void cacheFlow_flowArgIsEmpty_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(IllegalArgumentException.class, () -> pgJobStoreRepository.cacheFlow(" "));
    }

    @Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JSONBException {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        String flowJson = pgJobStoreRepository.jsonbContext.marshall(new FlowBuilder().build());
        FlowCacheEntity flowCacheEntity = pgJobStoreRepository.cacheFlow(flowJson);
        assertThat(flowCacheEntity, is(EXPECTED_FLOW_CACHE_ENTITY));
    }

    @Test
    public void cacheSink_sinkArgIsNull_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(NullPointerException.class, () -> pgJobStoreRepository.cacheSink(null));
    }

    @Test
    public void cacheSink_sinkArgIsEmpty_throws() {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThrows(IllegalArgumentException.class, () -> pgJobStoreRepository.cacheSink(" "));
    }

    @Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JSONBException {
        PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        String sinkJson = pgJobStoreRepository.jsonbContext.marshall(new SinkBuilder().build());
        SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(sinkJson);
        assertThat(sinkCacheEntity, is(EXPECTED_SINK_CACHE_ENTITY));
    }
}
