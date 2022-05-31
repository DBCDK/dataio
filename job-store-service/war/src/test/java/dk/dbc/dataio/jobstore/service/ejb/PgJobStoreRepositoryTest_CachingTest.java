package dk.dbc.dataio.jobstore.service.ejb;


import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class PgJobStoreRepositoryTest_CachingTest extends PgJobStoreBaseTest {
    @Test
    public void cacheFlow_flowArgIsNull_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.cacheFlow(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsEmpty_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.cacheFlow(" ");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JSONBException {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final String flowJson = pgJobStoreRepository.jsonbContext.marshall(new FlowBuilder().build());
        final FlowCacheEntity flowCacheEntity = pgJobStoreRepository.cacheFlow(flowJson);
        assertThat(flowCacheEntity, is(EXPECTED_FLOW_CACHE_ENTITY));
    }

    @Test
    public void cacheSink_sinkArgIsNull_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.cacheSink(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsEmpty_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.cacheSink(" ");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JSONBException {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final String sinkJson = pgJobStoreRepository.jsonbContext.marshall(new SinkBuilder().build());
        final SinkCacheEntity sinkCacheEntity = pgJobStoreRepository.cacheSink(sinkJson);
        assertThat(sinkCacheEntity, is(EXPECTED_SINK_CACHE_ENTITY));
    }
}
