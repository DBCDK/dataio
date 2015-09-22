/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.ejb;


import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PgJobStore_CachesTest extends PgJobStoreBaseTest {

    @Test
    public void cacheFlow_flowArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        try {
            pgJobStore.jobStoreRepository.cacheFlow(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsEmpty_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        try {
            pgJobStore.jobStoreRepository.cacheFlow(" ");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JobStoreException, JSONBException {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        final String flowJson = pgJobStore.jobStoreRepository.jsonbContext.marshall(new FlowBuilder().build());
        final FlowCacheEntity flowCacheEntity = pgJobStore.jobStoreRepository.cacheFlow(flowJson);
        assertThat(flowCacheEntity, is(EXPECTED_FLOW_CACHE_ENTITY));
    }

    @Test
    public void cacheSink_sinkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        try {
            pgJobStore.jobStoreRepository.cacheSink(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsEmpty_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        try {
            pgJobStore.jobStoreRepository.cacheSink(" ");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JobStoreException, JSONBException {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        final String sinkJson = pgJobStore.jobStoreRepository.jsonbContext.marshall(new SinkBuilder().build());
        final SinkCacheEntity sinkCacheEntity = pgJobStore.jobStoreRepository.cacheSink(sinkJson);
        assertThat(sinkCacheEntity, is(EXPECTED_SINK_CACHE_ENTITY));
    }
}