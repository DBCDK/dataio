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
import javax.ws.rs.core.Response;
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
     * Creates rerun request in the underlying data store
     * @param jobId ID of job to be rerun
     * @return RerunEntity with status code of rerun request creation
     * @throws JobStoreException on internal server error
     */
    public RerunEntity createJobRerun(int jobId) throws JobStoreException {
        try {
            final RerunEntity rerunEntity = new RerunEntity();
            final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
            if (jobEntity == null) {
                rerunEntity.withStatusCode(Response.Status.NOT_ACCEPTABLE);
                return rerunEntity;
            }

            final HarvesterToken harvesterToken = getHarvesterToken(jobEntity);
            if (harvesterToken == null) {
                rerunEntity.withStatusCode(Response.Status.PRECONDITION_FAILED);
                return rerunEntity;
            }

            if (!hasRerunImplementation(harvesterToken)) {
                rerunEntity.withStatusCode(Response.Status.NOT_IMPLEMENTED);
                return rerunEntity;
            }

            rerunsRepository.addWaiting(rerunEntity
                    .withHarvesterId(Math.toIntExact(harvesterToken.getId()))
                    .withJob(jobEntity)
                    .withStatusCode(Response.Status.CREATED));

            return rerunEntity;
        } finally {
            self().rerunNextIfAvailable();
        }
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
                 final int submitter = Math.toIntExact(rerunEntity.getJob().getSpecification().getSubmitterId());
                 final ArrayList<AddiMetaData> recordReferences = new ArrayList<>();
                 final JobExporter jobExporter = new JobExporter(entityManager);
                 jobExporter.exportBibliographicRecordIds(rerunEntity.getJob().getId())
                         .forEach(id -> recordReferences.add(new AddiMetaData()
                                 .withSubmitterNumber(submitter)
                                 .withBibliographicRecordId(id)));
                 rrHarvesterServiceConnectorBean.getConnector().createHarvestTask(rerunEntity.getHarvesterId(),
                         new HarvestRecordsRequest(recordReferences)
                                 .withBasedOnJob(rerunEntity.getJob().getId()));
                 rerunsRepository.remove(rerunEntity);
             } catch (RRHarvesterServiceConnectorException e) {
                 throw new JobStoreException("Communication with RR harvester service failed", e);
             } finally {
                 self().rerunNextIfAvailable();
             }
        }
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
                LOGGER.info("Unable to rerun job {} since no valid harvester token could be found",
                        jobEntity.getId(), e);
            }
        }
        return null;
    }

    private boolean hasRerunImplementation(HarvesterToken harvesterToken) {
        switch (harvesterToken.getHarvesterVariant()) {
            case RAW_REPO: return true;
            default: return false;
        }
    }
}
