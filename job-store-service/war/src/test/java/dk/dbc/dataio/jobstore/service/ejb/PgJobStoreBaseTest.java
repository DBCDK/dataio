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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.junit.Before;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class PgJobStoreBaseTest {
    protected final EntityManager entityManager = mock(EntityManager.class);
    protected final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    protected final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    protected static final FileStoreServiceConnectorUnexpectedStatusCodeException fileStoreUnexpectedException = new FileStoreServiceConnectorUnexpectedStatusCodeException("unexpected status code", 400);
    protected static final FileStoreUrn FILE_STORE_URN = FileStoreUrn.create("42");
    protected static final List<String> EXPECTED_DATA_ENTRIES = Arrays.asList(
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>first</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>second</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>third</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>fourth</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>fifth</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>sixth</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>seventh</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>eighth</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>ninth</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>tenth</record></records>"),
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>eleventh</record></records>"));
    protected static final int EXPECTED_NUMBER_OF_ITEMS = EXPECTED_DATA_ENTRIES.size();
    static final int EXPECTED_NUMBER_OF_CHUNKS = (int) Math.ceil((float) EXPECTED_NUMBER_OF_ITEMS / 10);
    protected final PgJobStoreRepository mockedJobStoreRepository = mock(PgJobStoreRepository.class);
    protected final JobQueueRepository mockedJobQueueReposity = mock(JobQueueRepository.class);
    protected final JobNotificationRepository mockedJobNotificationRepository = mock(JobNotificationRepository.class);

    protected static final FlowCacheEntity EXPECTED_FLOW_CACHE_ENTITY = mock(FlowCacheEntity.class);
    protected static final SinkCacheEntity EXPECTED_SINK_CACHE_ENTITY = mock(SinkCacheEntity.class);

    protected final static Sink EXPECTED_SINK = new SinkBuilder().build();
    protected final static Flow EXPECTED_FLOW = new FlowBuilder().build();
    protected final static Submitter EXPECTED_SUBMITTER = new SubmitterBuilder().build();

    protected static final int DEFAULT_JOB_ID = 1;
    protected static final int DEFAULT_CHUNK_ID = 0;
    protected static final short DEFAULT_ITEM_ID = 0;

    private final SessionContext sessionContext = mock(SessionContext.class);
    private final JobSchedulerBean jobSchedulerBean = mock(JobSchedulerBean.class);
    private final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnectorBean mockedFlowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final SessionContext mockedSessionContext = mock(SessionContext.class);

    @Before
    public void setupExpectations() throws JobStoreException, FlowStoreServiceConnectorException {
        final Query cacheFlowQuery = mock(Query.class);
        when(entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(cacheFlowQuery);
        when(cacheFlowQuery.getSingleResult()).thenReturn(EXPECTED_FLOW_CACHE_ENTITY);
        when(EXPECTED_FLOW_CACHE_ENTITY.getFlow()).thenReturn(EXPECTED_FLOW);

        final Query cacheSinkQuery = mock(Query.class);
        when(entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(cacheSinkQuery);
        when(cacheSinkQuery.getSingleResult()).thenReturn(EXPECTED_SINK_CACHE_ENTITY);
        when(EXPECTED_SINK_CACHE_ENTITY.getSink()).thenReturn(EXPECTED_SINK);

        when(entityManager.merge(any()))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    return args[0];
                });
    }


    protected PgJobStoreRepository newPgJobStoreReposity() {
        final PgJobStoreRepository pgJobStoreRepository = new PgJobStoreRepository();
        pgJobStoreRepository.entityManager = entityManager;

        return pgJobStoreRepository;
    }

    protected JobQueueRepository newJobQueueRepository() {
        final JobQueueRepository jobQueueRepository = new JobQueueRepository();
        jobQueueRepository.entityManager = entityManager;

        return jobQueueRepository;
    }

    protected PgJobStore newPgJobStore() {
        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.entityManager = entityManager;
        pgJobStore.jobStoreRepository = mockedJobStoreRepository;
        pgJobStore.jobQueueRepository = mockedJobQueueReposity;
        pgJobStore.jobNotificationRepository = mockedJobNotificationRepository;
        pgJobStore.jobSchedulerBean = jobSchedulerBean;
        pgJobStore.jobStoreRepository.entityManager = entityManager;
        pgJobStore.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        pgJobStore.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        pgJobStore.sessionContext = mockedSessionContext;
        when(sessionContext.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);
        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);
        when(mockedFlowStoreServiceConnectorBean.getConnector()).thenReturn(mockedFlowStoreServiceConnector);
        when(mockedSessionContext.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);

        return pgJobStore;
    }

    protected PgJobStore newPgJobStore(PgJobStoreRepository jobStoreRepository) {
        final PgJobStore pgJobStore = newPgJobStore();
        pgJobStore.entityManager = entityManager;
        pgJobStore.jobStoreRepository = jobStoreRepository;
        return pgJobStore;
    }

    protected String getXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<records>"
                + "<record>first</record>"
                + "<record>second</record>"
                + "<record>third</record>"
                + "<record>fourth</record>"
                + "<record>fifth</record>"
                + "<record>sixth</record>"
                + "<record>seventh</record>"
                + "<record>eighth</record>"
                + "<record>ninth</record>"
                + "<record>tenth</record>"
                + "<record>eleventh</record>"
                + "</records>";
    }

    protected State getUpdatedState(int numberOfItems, List<State.Phase> phasesDone) {
        final StateChange jobStateChange = new StateChange();
        final State jobState = new State();
        for (State.Phase phase : phasesDone) {
            jobStateChange.setPhase(phase)
                    .setSucceeded(numberOfItems)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
            jobState.updateState(jobStateChange);
        }
        return jobState;
    }

    protected JobEntity getJobEntity(int jobId) {
        final FlowCacheEntity mockedFlowCacheEntity = mock(FlowCacheEntity.class);
        final SinkCacheEntity mockedSinkCacheEntity = mock(SinkCacheEntity.class);

        final FlowStoreReference flowStoreReference = new FlowStoreReference(EXPECTED_SUBMITTER.getId(), EXPECTED_SUBMITTER.getVersion(), EXPECTED_SUBMITTER.getContent().getName());
        final FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, flowStoreReference);

        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(getJobSpecification());
        jobEntity.setCachedFlow(mockedFlowCacheEntity);
        jobEntity.setCachedSink(mockedSinkCacheEntity);
        jobEntity.setState(new State());
        jobEntity.setFlowStoreReferences(flowStoreReferences);

        when(entityManager.find(JobEntity.class, jobId)).thenReturn(jobEntity);

        return jobEntity;
    }

    private JobSpecification getJobSpecification() {
        return new JobSpecification()
                .withDataFile(FILE_STORE_URN.toString())
                .withMailForNotificationAboutProcessing("mail")
                .withMailForNotificationAboutVerification("mail");
    }

    protected Query whenCreateQueryThenReturn() {
        final Query query = mock(Query.class);
        when(query.setParameter(anyString(), anyInt())).thenReturn(query);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        return query;
    }

    protected Query whenCreateNativeQueryThenReturn() {
        final Query query = mock(Query.class);
        when(query.setParameter(anyString(), anyInt())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        return query;
    }
}