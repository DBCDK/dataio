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
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnector;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnectorException;
import dk.dbc.dataio.rrharvester.service.connector.ejb.RRHarvesterServiceConnectorBean;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ejb.SessionContext;
import javax.ws.rs.core.Response;
import java.util.Arrays;

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

    @Test
    public void jobDoesNotExistForRerun() throws JobStoreException {
        final JobRerunnerBean jobRerunnerBean = newJobRerunnerBean();
        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.createJobRerun(42));
        assertThat(rerun.getStatusCode(), is(Response.Status.NOT_ACCEPTABLE));
    }

    @Test
    public void jobHasNoHarvesterTokenForRerun() throws JobStoreException {
        final JobEntity job = newPersistedJobEntity();
        final JobRerunnerBean jobRerunnerBean = newJobRerunnerBean();
        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.createJobRerun(job.getId()));
        assertThat(rerun.getStatusCode(), is(Response.Status.PRECONDITION_FAILED));
    }

    @Test
    public void jobHasHarvesterTokenWithUnsupportedHarvesterVariant() {
        final JobEntity job = newJobEntity();
        job.setSpecification(new JobSpecificationBuilder()
                .setAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(new HarvesterToken()
                                .withHarvesterVariant(HarvesterToken.HarvesterVariant.TICKLE_REPO)
                                .withId(42)
                                .withVersion(1)
                                .toString()))
                .build());

        persist(job);

        final JobRerunnerBean jobRerunnerBean = newJobRerunnerBean();
        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.createJobRerun(job.getId()));
        assertThat(rerun.getStatusCode(), is(Response.Status.NOT_IMPLEMENTED));
    }

    @Test
    public void createsWaitingRerunTask() throws JobStoreException {
        final JobEntity job = newJobEntity();
        job.setSpecification(new JobSpecificationBuilder()
                .setAncestry(new JobSpecification.Ancestry()
                        .withHarvesterToken(new HarvesterToken()
                                .withHarvesterVariant(HarvesterToken.HarvesterVariant.RAW_REPO)
                                .withId(42)
                                .withVersion(1)
                                .toString()))
                .build());

        persist(job);

        final JobRerunnerBean jobRerunnerBean = newJobRerunnerBean();
        when(sessionContext.getBusinessObject(JobRerunnerBean.class)).thenReturn(mock(JobRerunnerBean.class));

        final RerunEntity rerun = persistenceContext.run(() -> jobRerunnerBean.createJobRerun(job.getId()));
        assertThat("status code", rerun.getStatusCode(), is(Response.Status.CREATED));
        assertThat("entity in database", entityManager.find(RerunEntity.class, rerun.getId()), is(notNullValue()));
    }

    @Test
    public void executesRerunTask() throws RRHarvesterServiceConnectorException {
        final JobEntity job = newPersistedJobEntity();
        final ChunkEntity chunk = newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));
        final ItemEntity item0 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 0));
        item0.setRecordInfo(new RecordInfo("id0"));
        persist(item0);
        final ItemEntity item1 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 1));
        item1.setRecordInfo(new RecordInfo("id1"));
        persist(item1);
        final ItemEntity item2 = newItemEntity(new ItemEntity.Key(job.getId(), chunk.getKey().getId(), (short) 2));
        item2.setRecordInfo(new RecordInfo("id2"));
        persist(item2);

        final RerunEntity rerun = newPersistedRerunEntity(job);

        final JobRerunnerBean jobRerunnerBean = newJobRerunnerBean();
        persistenceContext.run(jobRerunnerBean::rerunNextIfAvailable);
        assertThat("entity in database", entityManager.find(RerunEntity.class, rerun.getId()), is(nullValue()));

        final ArgumentCaptor<HarvestRecordsRequest> argumentCaptor = ArgumentCaptor.forClass(HarvestRecordsRequest.class);
        verify(rrHarvesterServiceConnector).createHarvestTask(eq(rerun.getHarvesterId().longValue()), argumentCaptor.capture());

        final HarvestRecordsRequest request = argumentCaptor.getValue();
        assertThat("request.getBasedOnJob()", request.getBasedOnJob(), is(job.getId()));
        final int submitter = (int) job.getSpecification().getSubmitterId();
        assertThat("request.getRecords", request.getRecords(), is(Arrays.asList(
                new AddiMetaData().withBibliographicRecordId(item0.getRecordInfo().getId()).withSubmitterNumber(submitter),
                new AddiMetaData().withBibliographicRecordId(item1.getRecordInfo().getId()).withSubmitterNumber(submitter),
                new AddiMetaData().withBibliographicRecordId(item2.getRecordInfo().getId()).withSubmitterNumber(submitter))));
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