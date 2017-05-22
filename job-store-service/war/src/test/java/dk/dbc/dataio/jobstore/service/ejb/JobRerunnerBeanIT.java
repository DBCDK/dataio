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

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnector;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnectorException;
import dk.dbc.dataio.rrharvester.service.connector.ejb.RRHarvesterServiceConnectorBean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ejb.SessionContext;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobRerunnerBeanIT extends AbstractJobStoreIT {
    private SessionContext sessionContext = mock(SessionContext.class);
    private RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean = mock(RRHarvesterServiceConnectorBean.class);
    private RRHarvesterServiceConnector rrHarvesterServiceConnector = mock(RRHarvesterServiceConnector.class);

    private final HarvesterToken rawRepoHarvesterToken = HarvesterToken.of("raw-repo:42:1");

    private JobRerunnerBean jobRerunnerBean;

    @Before
    public void initializeJobRerunnerBean() {
        jobRerunnerBean = newJobRerunnerBean();
    }

    @Test
    public void jobDoesNotExistForRerun() throws JobStoreException {
        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.requestJobRerun(42));
        assertThat(rerun, is(nullValue()));
    }

    @Test
    public void createsWaitingRerunTask() throws JobStoreException {
        final JobEntity job = newJobEntity();
        job.setSpecification(new JobSpecification()
                .withAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(new HarvesterToken()
                                .withHarvesterVariant(HarvesterToken.HarvesterVariant.RAW_REPO)
                                .withId(42)
                                .withVersion(1)
                                .toString()))
        );

        persist(job);

        final JobRerunnerBean jobRerunnerBean = newJobRerunnerBean();
        when(sessionContext.getBusinessObject(JobRerunnerBean.class)).thenReturn(mock(JobRerunnerBean.class));

        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.requestJobRerun(job.getId()));
        assertThat("entity in database", entityManager.find(RerunEntity.class, rerun.getId()), is(notNullValue()));
    }

    @Test
    public void rerunNextIfAvailable_removesRerunEntityAfterTaskCompletion() {
        final RerunEntity rerun = getRerunEntity();

        persistenceContext.run(jobRerunnerBean::rerunNextIfAvailable);
        assertThat("entity in database", entityManager.find(RerunEntity.class, rerun.getId()), is(nullValue()));
    }

    @Test
    public void rerunRawRepoHarvesterJob_exportingBibliographicRecordIds() throws RRHarvesterServiceConnectorException {
        final RerunEntity rerun = getRerunEntityForRawRepoHarvesterTask();
        final JobEntity job = rerun.getJob();

        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, rawRepoHarvesterToken));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(rrHarvesterServiceConnector).createHarvestTask(eq(rawRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        assertThat("request submitter", (long) request.getRecords().get(0).submitterNumber(),
                is(job.getSpecification().getSubmitterId()));
        assertThat("request bibliographic record IDs", request.getRecords()
                .stream().map(AddiMetaData::bibliographicRecordId).collect(Collectors.toList()),
                is(Arrays.asList("id0", "id1", "id2")));
    }

    @Test
    public void rerunRawRepoHarvesterJob_exportingBibliographicRecordIdsFromFailingItems() throws RRHarvesterServiceConnectorException {
        final RerunEntity rerun = getRerunEntityForRawRepoHarvesterTask();
        persistenceContext.run(() -> rerun.withIncludeFailedOnly(true));

        final JobEntity job = rerun.getJob();

        persistenceContext.run(() -> jobRerunnerBean.rerunHarvesterJob(rerun, rawRepoHarvesterToken));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(rrHarvesterServiceConnector).createHarvestTask(eq(rawRepoHarvesterToken.getId()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        assertThat("request submitter", (long) request.getRecords().get(0).submitterNumber(),
                is(job.getSpecification().getSubmitterId()));
        assertThat("request bibliographic record IDs", request.getRecords()
                .stream().map(AddiMetaData::bibliographicRecordId).collect(Collectors.toList()),
                is(Arrays.asList("id1", "id2")));
    }

    private RerunEntity getRerunEntityForRawRepoHarvesterTask() {
         return getRerunEntity(createJobSpecification()
                 .withAncestry(new JobSpecification.Ancestry()
                         .withHarvesterToken(rawRepoHarvesterToken.toString())));
    }

    private RerunEntity getRerunEntity() {
        return getRerunEntity(createJobSpecification());
    }

    private RerunEntity getRerunEntity(JobSpecification jobSpecification) {
        final JobEntity job = newJobEntity();
        job.setSpecification(jobSpecification);
        persist(job);

        final ChunkEntity chunk = newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));

        final ItemEntity item0 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 0));
        item0.setRecordInfo(new RecordInfo("id0"));
        persist(item0);

        final ItemEntity item1 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 1));
        item1.setRecordInfo(new RecordInfo("id1"));
        State state = new State(item1.getState());
        state.updateState(new StateChange()
                        .setPhase(State.Phase.PROCESSING)
                        .incFailed(1));
        item1.setState(state);
        persist(item1);

        final ItemEntity item2 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 2));
        item2.setRecordInfo(new RecordInfo("id2"));
        state = new State(item2.getState());
        state.updateState(new StateChange()
                .setPhase(State.Phase.DELIVERING)
                .incFailed(1));
        item2.setState(state);
        persist(item2);

        return newPersistedRerunEntity(job);
    }

    private JobRerunnerBean newJobRerunnerBean() {
        final JobRerunnerBean jobRerunnerBean = new JobRerunnerBean();
        jobRerunnerBean.rerunsRepository = newRerunsRepository();
        jobRerunnerBean.rrHarvesterServiceConnectorBean = rrHarvesterServiceConnectorBean;
        jobRerunnerBean.sessionContext = sessionContext;
        jobRerunnerBean.entityManager = entityManager;
        when(rrHarvesterServiceConnectorBean.getConnector()).thenReturn(rrHarvesterServiceConnector);
        when(sessionContext.getBusinessObject(JobRerunnerBean.class)).thenReturn(jobRerunnerBean);
        return jobRerunnerBean;
    }
}
