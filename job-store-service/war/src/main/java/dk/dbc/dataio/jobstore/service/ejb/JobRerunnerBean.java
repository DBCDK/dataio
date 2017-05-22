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
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.service.util.JobExporter;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.rrharvester.service.connector.RRHarvesterServiceConnectorException;
import dk.dbc.dataio.rrharvester.service.connector.ejb.RRHarvesterServiceConnectorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Optional;

/**
 * This stateless Enterprise Java Bean (EJB) handles job rerun tasks
 */
@Stateless
public class JobRerunnerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRerunnerBean.class);

    @EJB RerunsRepository rerunsRepository;
    @EJB RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean;
    @Resource SessionContext sessionContext;
    @Inject @JobstoreDB EntityManager entityManager;

    /**
     * Creates rerun task in the underlying data store
     * @param jobId ID of job to be rerun
     * @return RerunEntity or null if rerun task could not be created
     * @throws JobStoreException on internal server error
     */
    public RerunEntity requestJobRerun(int jobId) throws JobStoreException {
        try {
            return createRerunEntity(jobId);
        } finally {
            self().rerunNextIfAvailable();
        }
    }

    /**
     * Creates rerun task in the underlying data store for failed items only
     * @param jobId ID of job to be rerun
     * @return RerunEntity or null if rerun task could not be created
     * @throws JobStoreException on internal server error
     */
    public RerunEntity requestJobFailedItemsRerun(int jobId) throws JobStoreException {
        try {
            final RerunEntity rerunEntity = createRerunEntity(jobId);
            if (rerunEntity != null) {
                rerunEntity.withIncludeFailedOnly(true);
            }
            return rerunEntity;
        } finally {
            self().rerunNextIfAvailable();
        }
    }

    private RerunEntity createRerunEntity(int jobId) {
        final RerunEntity rerunEntity = new RerunEntity();
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
        if (jobEntity == null) {
            return null;
        }

        rerunsRepository.addWaiting(rerunEntity.withJob(jobEntity));
        return rerunEntity;
    }

    /**
     * Attempts to process next rerun task in line
     * @throws JobStoreException on internal server error
     */
    @Stopwatch
    @Asynchronous
    public void rerunNextIfAvailable() throws JobStoreException {
        final Optional<RerunEntity> rerun = rerunsRepository.seizeHeadOfQueueIfWaiting();
        if (rerun.isPresent()) {
            final RerunEntity rerunEntity = rerun.get();
             try {
                 final HarvesterToken harvesterToken = getHarvesterToken(rerunEntity.getJob());
                 if (harvesterToken != null) {
                     rerunHarvesterJob(rerunEntity, harvesterToken);
                 } else {
                     rerunJob(rerunEntity);
                 }
                 rerunsRepository.remove(rerunEntity);
             } catch (Exception e) {
                 rerunsRepository.reset(rerunEntity);
             } finally {
                 self().rerunNextIfAvailable();
             }
        }
    }

    void rerunHarvesterJob(RerunEntity rerunEntity, HarvesterToken harvesterToken) throws JobStoreException {
        switch (harvesterToken.getHarvesterVariant()) {
            case RAW_REPO: rerunRawRepoHarvesterJob(rerunEntity, harvesterToken.getId());
                    break;
            default: rerunJob(rerunEntity);
        }
    }

    private void rerunRawRepoHarvesterJob(RerunEntity rerunEntity, long harvesterId) throws JobStoreException {
        try {
            final JobEntity job = rerunEntity.getJob();
            final int submitter = Math.toIntExact(job.getSpecification().getSubmitterId());

            final ArrayList<AddiMetaData> recordReferences = new ArrayList<>();
            final JobExporter jobExporter = new JobExporter(entityManager);
            if (rerunEntity.isIncludeFailedOnly()) {
                jobExporter.exportFailedItemsBibliographicRecordIds(job.getId())
                        .forEach(id -> recordReferences.add(new AddiMetaData()
                                .withSubmitterNumber(submitter)
                                .withBibliographicRecordId(id)));
            } else {
                jobExporter.exportItemsBibliographicRecordIds(job.getId())
                        .forEach(id -> recordReferences.add(new AddiMetaData()
                                .withSubmitterNumber(submitter)
                                .withBibliographicRecordId(id)));
            }
            rrHarvesterServiceConnectorBean.getConnector().createHarvestTask(harvesterId,
                    new HarvestRecordsRequest(recordReferences)
                            .withBasedOnJob(job.getId()));
        } catch (RRHarvesterServiceConnectorException e) {
            throw new JobStoreException("Communication with RR harvester service failed", e);
        }
    }

    private void rerunJob(RerunEntity rerunEntity) {
        LOGGER.warn("No implementation exists to rerun job with ID {}", rerunEntity.getJob().getId());
    }

    private JobRerunnerBean self() {
        return sessionContext.getBusinessObject(JobRerunnerBean.class);
    }

    private HarvesterToken getHarvesterToken(JobEntity jobEntity) {
        final JobSpecification.Ancestry ancestry = jobEntity.getSpecification().getAncestry();
        if (ancestry != null) {
            try {
                return HarvesterToken.of(ancestry.getHarvesterToken());
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }
}
