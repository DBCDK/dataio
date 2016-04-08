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

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;
import types.TestableAddJobParam;
import types.TestableAddJobParamBuilder;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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
     * Given    : a job store with empty flowcache
     * When     : creating new job entities with non-ACCTEST type and caching referencing flows which only differs on
     *            their "next" components
     * Then     : the flows are trimmed resulting in cache hits
     * And When :  creating new job entities with type ACCTEST
     * Then     : the flow is not trimmed resulting in cache insert.
     */
    @Test
    public void createJobEntity_trimsNonAcctestFlows() throws SQLException {
        // Given...
        int nextRevision = 1;
        for (JobSpecification.Type type : JobSpecification.Type.values()) {
            if (type == JobSpecification.Type.ACCTEST)
                continue;

            final JobSpecification jobSpecification = getJobSpecification(type);
            final Flow flow = getFlowWithNextFlowComponent(nextRevision++);

            final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                    .setJobSpecification(jobSpecification)
                    .setFlow(flow).build();

            // When...
            persistenceContext.run(() -> pgJobStoreRepository.createJobEntity(testableAddJobParam));

            // Then... the flows are trimmed resulting in cache hits
            assertThat("flow cache table size when flow is trimmed", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(1L));
        }
        final JobSpecification jobSpecification = getJobSpecification(JobSpecification.Type.ACCTEST);
        final Flow flow = getFlowWithNextFlowComponent(nextRevision++);

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setJobSpecification(jobSpecification)
                .setFlow(flow).build();

        // And When...
        persistenceContext.run(() -> pgJobStoreRepository.createJobEntity(testableAddJobParam));

        // Then... the flow is not trimmed resulting in cache insert.
        assertThat("flow cache table size when flow is not trimmed", getSizeOfTable(FLOW_CACHE_TABLE_NAME), is(2L));
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

    /*
     * Private methods
     */

    private JobSpecification getJobSpecification(JobSpecification.Type type) {
        try {
            return new JobSpecificationBuilder().setType(type).setDataFile(FileStoreUrn.create("42").toString()).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private Flow getFlowWithNextFlowComponent(int nextRevision) {
        return new FlowBuilder()
                .setContent(new FlowContentBuilder()
                        .setComponents(Collections.singletonList(getFlowComponentWithNext(nextRevision)))
                        .build())
                .build();
    }

    private FlowComponent getFlowComponentWithNext(int nextRevision) {
        return new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next_" + nextRevision).build()).build();
    }
}
