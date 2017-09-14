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

import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.harvester.connector.ejb.TickleHarvesterServiceConnectorBean;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.util.JobExporter;
import dk.dbc.dataio.jobstore.service.util.MailNotification;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.rrharvester.service.connector.ejb.RRHarvesterServiceConnectorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Session;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Optional;

/**
 * This stateless Enterprise Java Bean (EJB) handles job rerun tasks
 */
@Stateless
public class JobRerunnerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRerunnerBean.class);

    @EJB RerunsRepository rerunsRepository;
    @EJB RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean;
    @EJB TickleHarvesterServiceConnectorBean tickleHarvesterServiceConnectorBean;
    @EJB PgJobStore pgJobStore;
    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @Resource SessionContext sessionContext;
    @Inject @JobstoreDB EntityManager entityManager;

    @Resource(lookup = JndiConstants.MAIL_RESOURCE_JOBSTORE_NOTIFICATIONS)
    Session mailSession;

    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Creates rerun task in the underlying data store
     * @param jobId ID of job to be rerun
     * @return RerunEntity or null if rerun task could not be created
     * @throws JobStoreException as {@link InvalidInputException} subtype if job could not be found
     * @throws JobStoreException on internal server error
     */
    public RerunEntity requestJobRerun(int jobId) throws JobStoreException {
        try {
            return createRerunEntity(getJobEntity(jobId), false);
        } finally {
            self().rerunNextIfAvailable();
        }
    }

    /**
     * Creates rerun task in the underlying data store for failed items only
     * @param jobId ID of job to be rerun
     * @return RerunEntity or null if rerun task could not be created
     * @throws JobStoreException as {@link InvalidInputException} subtype if job could not be found
     * @throws JobStoreException on internal server error
     */
    public RerunEntity requestJobFailedItemsRerun(int jobId) throws JobStoreException {
        try {
            return createRerunEntity(getJobEntity(jobId), true);
        } finally {
            self().rerunNextIfAvailable();
        }
    }

    private RerunEntity createRerunEntity(JobEntity job, boolean includeFailedOnly) throws InvalidInputException {
        final RerunEntity rerunEntity = new RerunEntity()
                .withJob(job)
                .withIncludeFailedOnly(includeFailedOnly);
        rerunsRepository.addWaiting(rerunEntity);
        return rerunEntity;
    }

    private JobEntity getJobEntity(int jobId) throws InvalidInputException {
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
        if (jobEntity == null) {
            throw new InvalidInputException(String.format("job %d not found", jobId),
                    new JobError(JobError.Code.INVALID_JOB_IDENTIFIER));
        }
        return jobEntity;
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
                 // catching a null pointer here could lead to an infinite
                 // loop leading to a stack overflow
                 LOGGER.error("Caught exception: ", e);
                 try {
                     Thread.sleep(60000);
                 } catch(InterruptedException e2) {}
             } finally {
                 self().rerunNextIfAvailable();
             }
        }
    }

    void rerunHarvesterJob(RerunEntity rerunEntity, HarvesterToken harvesterToken) throws JobStoreException {
        switch (harvesterToken.getHarvesterVariant()) {
            case RAW_REPO: rerunRawRepoHarvesterJob(rerunEntity, harvesterToken.getId());
                    break;
            case TICKLE_REPO: rerunTickleRepoHarvesterJob(rerunEntity, harvesterToken.getId());
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
            try (JobExporter.JobExport<RecordInfo> bibliographicRecordIds = rerunEntity.isIncludeFailedOnly() ?
                    jobExporter.exportFailedItemsRecordInfo(job.getId()) :
                    jobExporter.exportItemsRecordInfo(job.getId())) {
                bibliographicRecordIds.forEach(recordInfo -> {
                    if (recordInfo != null) {
                        recordReferences.add(new AddiMetaData()
                                .withSubmitterNumber(submitter)
                                .withBibliographicRecordId(recordInfo.getId())
                                .withPid(recordInfo.getPid()));
                    }
                });
            }
            rrHarvesterServiceConnectorBean.getConnector().createHarvestTask(harvesterId,
                    new HarvestRecordsRequest(recordReferences)
                            .withBasedOnJob(job.getId()));
        } catch (HarvesterTaskServiceConnectorException e) {
            throw new JobStoreException("Communication with RR harvester service failed", e);
        }
    }

    private void rerunTickleRepoHarvesterJob(RerunEntity rerunEntity, long harvesterId) throws JobStoreException {
        try {
            final JobEntity job = rerunEntity.getJob();
            final ArrayList<AddiMetaData> recordReferences = new ArrayList<>();
            final JobExporter jobExporter = new JobExporter(entityManager);
            try (JobExporter.JobExport<String> bibliographicRecordIds = rerunEntity.isIncludeFailedOnly() ?
                    jobExporter.exportFailedItemsBibliographicRecordIds(job.getId()) :
                    jobExporter.exportItemsBibliographicRecordIds(job.getId())) {
                bibliographicRecordIds.forEach(id -> {
                    if (id != null) {
                        recordReferences.add(new AddiMetaData().withBibliographicRecordId(id));
                    }
                });
            }
            tickleHarvesterServiceConnectorBean.getConnector().createHarvestTask(harvesterId,
                    new HarvestRecordsRequest(recordReferences)
                            .withBasedOnJob(job.getId()));
        } catch (HarvesterTaskServiceConnectorException e) {
            throw new JobStoreException("Communication with tickle harvester service failed", e);
        }
    }

    private void rerunJob(RerunEntity rerunEntity) throws JobStoreException {
        final JobExporter jobExporter = new JobExporter(entityManager);
        final JobEntity job = rerunEntity.getJob();

        // marshall/unmarshall to avoid changes made to specification becoming visible on original entity
        final JobSpecification jobSpecification;
        try {
            jobSpecification = jsonbContext.unmarshall(
                    jsonbContext.marshall(job.getSpecification()), JobSpecification.class);
        } catch (JSONBException e) {
            throw new JobStoreException("Unable to copy specification from job " + job.getId(), e);
        }
        JobSpecification.Ancestry ancestry = jobSpecification.getAncestry();
        if (ancestry != null) {
            ancestry.withPreviousJobId(job.getId());
        } else {
            ancestry = new JobSpecification.Ancestry()
                .withPreviousJobId(job.getId());
            jobSpecification.withAncestry(ancestry);
        }
        final String notificationDestination = getNotificationDestination(rerunEntity.getJob());
        jobSpecification
                .withMailForNotificationAboutVerification(notificationDestination)
                .withMailForNotificationAboutProcessing(notificationDestination);

        final JobInputStream jobInputStream = new JobInputStream(jobSpecification);
        final AddJobParam addJobParam = new AddJobParam(jobInputStream,
            flowStoreServiceConnectorBean.getConnector());

        final BitSet bitSet = new BitSet();
        try (JobExporter.JobExport<Integer> positions = rerunEntity.isIncludeFailedOnly() ?
                jobExporter.exportFailedItemsPositionsInDatafile(rerunEntity.getJob().getId()) :
                jobExporter.exportItemsPositionsInDatafile(rerunEntity.getJob().getId())) {
            positions.forEach(position -> {
                if (position != null) {
                    bitSet.set(position);
                }
            });
        }
        logBitSet(rerunEntity.getJob().getId(), bitSet);
        pgJobStore.addJob(addJobParam, bitSet.toByteArray());
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

    private String getNotificationDestination(JobEntity job) {
        final JobSpecification jobSpecification = job.getSpecification();
        if (jobSpecification != null) {
            if (!(MailNotification.isUndefined(jobSpecification.getMailForNotificationAboutVerification())
                    && MailNotification.isUndefined(jobSpecification.getMailForNotificationAboutProcessing()))) {
                return mailSession.getProperty("mail.to.fallback");
            }
        }
        return Constants.MISSING_FIELD_VALUE;
    }

    public static void logBitSet(int jobId, BitSet bitSet) {
        if (LOGGER.isDebugEnabled()) {
            if (bitSet.size() > 0) {
                final StringBuilder str = new StringBuilder("[");
                boolean first = true;
                for (int i = 0; i < bitSet.size(); i++) {
                    if (bitSet.get(i)) {
                        if (!first) {
                            str.append(", ");
                        }
                        str.append(i);
                        first = false;
                    }
                }
                str.append("]");
                LOGGER.debug("logBitSet: job {} include set {}", jobId, str.toString());
            }
        }
    }
}
