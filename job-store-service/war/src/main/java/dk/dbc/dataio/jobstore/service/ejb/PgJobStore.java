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
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.param.AddAccTestJobParam;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the job-store database through persistence layer
 */
@Stateless
public class PgJobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStore.class);
    private static final int MAX_NUMBER_OF_JOB_RETRIES = 1;
    private static final long FOUR_GIBIBYTE = 4 * 1024 * 1024 * 1024L;

    /* These instances are not private otherwise they were not accessible from automatic test */
    @EJB JobSchedulerBean jobSchedulerBean;
    @Inject FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @Inject FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB PgJobStoreRepository jobStoreRepository;
    @EJB JobQueueRepository jobQueueRepository;
    @EJB JobNotificationRepository jobNotificationRepository;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Resource SessionContext sessionContext;

    /**
     * Adds new job in the underlying data store from given job input stream, after attempting to retrieve
     * required referenced objects through addJobParam.
     *
     * @param jobInputStream, containing information needed to create job, chunk and item entities
     * @return information snapshot of added job
     * @throws JobStoreException on failure to add job
     */
    @Stopwatch
    public JobInfoSnapshot addAndScheduleJob(JobInputStream jobInputStream) throws JobStoreException {
        AddJobParam param = new AddJobParam(jobInputStream, flowStoreServiceConnectorBean.getConnector());
        return addJob(param);
    }

    @Stopwatch
    public JobInfoSnapshot addAndScheduleAccTestJob(AccTestJobInputStream jobInputStream) throws JobStoreException {
        AddAccTestJobParam param = new AddAccTestJobParam(jobInputStream, flowStoreServiceConnectorBean.getConnector());
        return addJob(param);
    }

    /**
     * Adds new job job, chunk and item entities in the underlying data store from given job input stream
     * @param addJobParam containing the elements required to create a new job as well as a list of Diagnostics.
     *                    If the list contains any diagnostic with level FATAL, the job will be marked as finished
     *                    before partitioning is attempted.
     * @return information snapshot of added job
     * @throws JobStoreException on failure to add job
     */
    @Stopwatch
    public JobInfoSnapshot addJob(AddJobParam addJobParam) throws JobStoreException {
        return addJob(addJobParam, null);
    }

    /**
     * Adds new job job, chunk and item entities in the underlying data store from given job input stream
     * @param addJobParam containing the elements required to create a new job as well as a list of Diagnostics.
     *                    If the list contains any diagnostic with level FATAL, the job will be marked as finished
     *                    before partitioning is attempted.
     * @param includeFilter bitset to filter out records, used for rerunning jobs with only failed items
     * @return information snapshot of added job
     * @throws JobStoreException on failure to add job
     */
    @Stopwatch
    public JobInfoSnapshot addJob(AddJobParam addJobParam, byte[] includeFilter) throws JobStoreException {
        // Creates job entity in its own transactional scope to enable external visibility
        JobEntity jobEntity = jobStoreRepository.createJobEntity(addJobParam);
        LOGGER.info("addJob(): adding job with job ID: {}", jobEntity.getId());

        if (!jobEntity.hasFatalError()) {
            final Sink sink = jobEntity.getCachedSink().getSink();
            jobQueueRepository.addWaiting(new JobQueueEntity()
                    .withJob(jobEntity)
                    .withSinkId(sink.getId())
                    .withTypeOfDataPartitioner(addJobParam.getTypeOfDataPartitioner())
                    .withIncludeFilter(includeFilter));

            self().partitionNextJobForSinkIfAvailable(sink);
        } else {
            final Submitter submitter = addJobParam.getSubmitter();
            if (submitter == null || submitter.getContent().isEnabled()) {
                addNotificationIfSpecificationHasDestination(Notification.Type.JOB_CREATED, jobEntity);
            }
        }

        return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
    }



    private PgJobStore self() {
        return sessionContext.getBusinessObject(PgJobStore.class);
    }

    /**
     * Attempts to partition next job in line for a given {@link Sink}
     * @param sink {@link Sink} for which a job is to be partitioned
     */
    @Stopwatch
    @Asynchronous
    public void partitionNextJobForSinkIfAvailable(Sink sink) {
        final Optional<JobQueueEntity> nextToPartition = jobQueueRepository.seizeHeadOfQueueIfWaiting(sink);
        if (nextToPartition.isPresent()) {
            final JobQueueEntity jobQueueEntity = nextToPartition.get();
            try {
                BitSet includeFilter = null;
                if (jobQueueEntity.getIncludeFilter() != null) {
                    includeFilter = BitSet.valueOf(jobQueueEntity.getIncludeFilter());
                    JobRerunnerBean.logBitSet(jobQueueEntity.getJob().getId(), includeFilter);
                }

                final PartitioningParam param = new PartitioningParam(jobQueueEntity.getJob(),
                        fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(),
                        entityManager, jobQueueEntity.getTypeOfDataPartitioner(), includeFilter);

                if (!param.getDiagnostics().isEmpty()) {
                    abortJob(entityManager.merge(jobQueueEntity.getJob()), param.getDiagnostics());
                    jobQueueRepository.remove(jobQueueEntity);
                } else {
                    final Partitioning partitioning = handlePartitioning(param);
                    if (partitioning.hasFailedUnexpectedly()) {
                        if (partitioning.hasKnownFailure(Partitioning.KnownFailure.PREMATURE_END_OF_DATA)
                                // Data partitioners may throw PrematureEndOfDataException without cause,
                                // but a lost connection will always include an IOException.
                                && partitioning.getFailure().getCause() != null
                                && jobQueueEntity.getRetries() < MAX_NUMBER_OF_JOB_RETRIES) {
                            // Partitioning may have failed because of a lost filestore connection.
                            jobQueueRepository.retry(jobQueueEntity);
                        } else if (partitioning.hasKnownFailure(Partitioning.KnownFailure.TRANSACTION_ROLLED_BACK_LOCAL)) {
                            LOGGER.error("Lost current transaction while partitioning job {}, rescheduling and restarting",
                                    jobQueueEntity.getJob().getId(), partitioning.getFailure());
                            jobSchedulerBean.ensureLastChunkIsScheduled(jobQueueEntity.getJob().getId());
                            jobQueueRepository.retry(jobQueueEntity);
                        } else {
                            abortJobDueToUnforeseenFailuresDuringPartitioning(jobQueueEntity, partitioning.getFailure());
                        }
                    } else {
                        jobQueueRepository.remove(jobQueueEntity);
                    }
                }
            } catch (Throwable e) {
                if (e instanceof PrematureEndOfDataException
                        && jobQueueEntity.getRetries() < MAX_NUMBER_OF_JOB_RETRIES) {
                    jobQueueRepository.retry(jobQueueEntity);
                } else {
                    abortJobDueToUnforeseenFailuresDuringPartitioning(jobQueueEntity, e);
                }
            } finally {
                self().partitionNextJobForSinkIfAvailable(sink);
            }
        }
    }

    private void abortJobDueToUnforeseenFailuresDuringPartitioning(JobQueueEntity jobQueueEntity, Throwable e) {
        LOGGER.error(String.format("unexpected Exception caught while partitioning job %d", jobQueueEntity.getJob().getId()), e);
        abortJob(jobQueueEntity.getJob().getId(), "unexpected exception caught while partitioning job", e);
        jobQueueRepository.remove(jobQueueEntity);
    }

    /**
     * Deciphers if the job to be created is for preview only
     * @param param containing preview only information
     * @return partitioning
     */
    protected Partitioning handlePartitioning(PartitioningParam param) {
        if(param.isPreviewOnly()) {
            return self().preview(param);
        } else {
            return self().partition(param);
        }
    }

    /**
     * Partitions a job into chunks as specified by given partitioning parameter
     * @param partitioningParam containing the state required to partition a new job
     * @return partitioning result
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Partitioning partition(PartitioningParam partitioningParam) {
        JobEntity jobEntity = partitioningParam.getJobEntity();
        try {
            final Partitioning partitioning = new Partitioning();
            try {
                jobEntity = partitionJobIntoChunksAndItems(jobEntity, partitioningParam);
                jobEntity = verifyJobPartitioning(jobEntity, partitioningParam);
                jobSchedulerBean.markJobPartitioned(jobEntity);
                jobEntity = self().finalizePartitioning(jobEntity);
            } catch (Exception e) {
                partitioning.withFailure(e);
                return partitioning;
            }
            return partitioning.withJobEntity(jobEntity);
        } finally {
            partitioningParam.closeDataFile();
            logTimerMessage(jobEntity);
        }
    }


    /**
     * Adds information regarding number of failed and succeeded items on a job entity and sets time of completion
     * @param partitioningParam containing the required data partitioner
     * @return partitioning result
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Partitioning preview(PartitioningParam partitioningParam) {
        JobEntity jobEntity = partitioningParam.getJobEntity();
        try {
            final Partitioning partitioning = new Partitioning();
            try {
                jobEntity = jobStoreRepository.preview(jobEntity.getId(), partitioningParam.getDataPartitioner());
                jobEntity = verifyJobPartitioning(jobEntity, partitioningParam);
                jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            } catch (Exception e) {
                partitioning.withFailure(e);
                return partitioning;
            }
            return partitioning.withJobEntity(jobEntity);
        } finally {
            partitioningParam.closeDataFile();
        }
    }

    /**
     * finalizePartitioning and test for 0 chunks.
     * @param job used to parse info from  partitionJobIntoChunksAndItems and verifyJobPartitioning to this
     * @return updated jobEntity
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public JobEntity finalizePartitioning(JobEntity job) {
        final JobEntity jobEntity = entityManager.find(JobEntity.class, job.getId(), LockModeType.PESSIMISTIC_WRITE);
        final State jobState = endPartitioningPhase(jobEntity);

        // If db entity does not have fatal error, load them
        if (job.hasFatalError() && !jobEntity.hasFatalError())  {
            abortJob(jobEntity, job.getState().getDiagnostics());
        }

        if (jobEntity.getNumberOfChunks() == 0) {
            completeZeroChunkJob(jobEntity);
        }

        addNotificationIfSpecificationHasDestination(Notification.Type.JOB_CREATED, jobEntity);

        // Due to asynchronous operations processing and delivering phases may complete before partitioning is marked as done.
        if (jobState.allPhasesAreDone()) {
            jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            addNotificationIfSpecificationHasDestination(Notification.Type.JOB_COMPLETED, jobEntity);
            logTimerMessage(jobEntity);
        }

        entityManager.flush();
        return jobEntity;
    }

    private State endPartitioningPhase(JobEntity job) {
        final Date now = new Date();

        State jobState = jobStoreRepository.updateJobEntityState(job, new StateChange()
                .setPhase(State.Phase.PARTITIONING).setEndDate(now));

        final int numberOfPartitionedItems = jobState.getPhase(State.Phase.PARTITIONING).getNumberOfItems();
        if (numberOfPartitionedItems == jobState.getPhase(State.Phase.PROCESSING).getNumberOfItems()) {
            jobState = jobStoreRepository.updateJobEntityState(job, new StateChange()
                    .setPhase(State.Phase.PROCESSING).setEndDate(now));
        }
        if (numberOfPartitionedItems == jobState.getPhase(State.Phase.DELIVERING).getNumberOfItems()) {
            jobState = jobStoreRepository.updateJobEntityState(job, new StateChange()
                    .setPhase(State.Phase.DELIVERING).setEndDate(now));
        }
        return jobState;
    }

    private JobEntity partitionJobIntoChunksAndItems(JobEntity job, PartitioningParam partitioningParam) throws JobStoreException {
        // Attempt partitioning only if no fatal error has occurred
        if (!job.hasFatalError()) {
            final List<Diagnostic> abortDiagnostics = new ArrayList<>(0);

            LOGGER.info("Partitioning job {}", job.getId());

            int chunkId = 0;
            ChunkEntity chunkEntity;

            if( job.getNumberOfChunks() > 0 ) {
                LOGGER.info("Resuming Partition of Job {} after {} chunks",job.getId(), job.getNumberOfChunks());
                chunkId=job.getNumberOfChunks();
                partitioningParam.getDataPartitioner().drainItems( job.getNumberOfItems() + job.getSkipped());
            }

            long submitterId = partitioningParam.getJobEntity().getSpecification().getSubmitterId();
            do {
                // Creates each chunk entity (and associated item entities) in its own
                // transactional scope to enable external visibility of job creation progress
                chunkEntity = jobStoreRepository.createChunkEntity(submitterId, job.getId(), chunkId, Constants.CHUNK_MAX_SIZE,
                        partitioningParam.getDataPartitioner(),
                        partitioningParam.getKeyGenerator(),
                        job.getSpecification().getDataFile());

                if (chunkEntity == null) { // no more chunks
                    break;
                }
                ++chunkId;

                if (chunkEntity.getState().fatalDiagnosticExists()) {
                    // Partitioning resulted in unrecoverable error - set diagnostic to force job abortion
                    abortDiagnostics.addAll(chunkEntity.getState().getDiagnostics());
                    break;
                }
                jobSchedulerBean.scheduleChunk(chunkEntity, job);

            } while (true);

            if (!abortDiagnostics.isEmpty()) {
                job = abortJob(job, abortDiagnostics);
            }

            // update numberOfChunks added for use bye partition
            job.setNumberOfChunks( chunkId );

        }
        return job;
    }

    /* Verifies that the input stream was processed entirely during partitioning */
    private JobEntity verifyJobPartitioning(JobEntity job, PartitioningParam partitioningParam) {
        // Verify partitioning only if no fatal error has occurred
        if (!job.hasFatalError()) {
            final List<Diagnostic> abortDiagnostics = new ArrayList<>(0);
            try {
                compareByteSize(partitioningParam.getDataFileId(), partitioningParam.getDataPartitioner());
            } catch (Exception exception) {
                final Diagnostic diagnostic = ObjectFactory.buildFatalDiagnostic(String.format(
                        "Partitioning succeeded but validation 'compareByteSize' failed: %s", exception.getMessage()),
                        exception);

                abortDiagnostics.add(diagnostic);
                if (!abortDiagnostics.isEmpty()) {
                    job = abortJob(job, abortDiagnostics);
                }
            }
        }
        return job;
    }

    /**
     * Adds chunk by updating existing items, chunk and job entities in the underlying data store.
     * @param chunk chunk
     * @return information snapshot of updated job
     * @throws NullPointerException if given null-valued chunk argument
     * @throws DuplicateChunkException if attempting to update already existing chunk
     * @throws InvalidInputException if unable to find referenced items, if chunk belongs to PARTITIONING phase
     * or if chunk contains a number of items not matching that of the internal chunk entity
     * @throws JobStoreException if unable to find referenced chunk or job entities
     */
    @Stopwatch
    public JobInfoSnapshot addChunk(Chunk chunk) throws NullPointerException, JobStoreException {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        LOGGER.info("addChunk: adding {} chunk {}/{}", chunk.getType(), chunk.getJobId(), chunk.getChunkId());

        final ChunkEntity.Key chunkKey =  new ChunkEntity.Key((int) chunk.getChunkId(), (int) chunk.getJobId());
        final ChunkEntity chunkEntity = jobStoreRepository.getExclusiveAccessFor(ChunkEntity.class, chunkKey);

        if (chunkEntity == null) {
            throw new JobStoreException(String.format("ChunkEntity.%s could not be found", chunkKey));
        }

        // update items
        final PgJobStoreRepository.ChunkItemEntities chunkItemEntities = jobStoreRepository.updateChunkItemEntities(chunk);

        if (chunkItemEntities.size() == chunkEntity.getNumberOfItems()) {
            // update chunk

            final State.Phase phase = chunkItemEntities.chunkStateChange.getPhase();
            final Date chunkPhaseBeginDate = chunkItemEntities.entities.get(0).getState().getPhase(phase).getBeginDate();
            final Date chunkPhaseEndDate = chunkItemEntities.entities.get(chunkItemEntities.size() - 1).getState().getPhase(phase).getEndDate();
            final StateChange chunkStateChange = chunkItemEntities.chunkStateChange
                    .setBeginDate(chunkPhaseBeginDate)
                    .setEndDate(chunkPhaseEndDate);

            final State chunkState = updateChunkEntityState(chunkEntity, chunkStateChange);
            if(chunkState.allPhasesAreDone()) {
                chunkEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            }

            // update job
            final JobEntity jobEntity = jobStoreRepository.getExclusiveAccessFor(JobEntity.class, chunkEntity.getKey().getJobId());
            if (jobEntity == null) {
                throw new JobStoreException(String.format("JobEntity.%d could not be found", chunkEntity.getKey().getJobId()));
            }
            final State jobState = jobStoreRepository.updateJobEntityState(jobEntity, chunkStateChange.setBeginDate(null).setEndDate(null));
            if (jobState.allPhasesAreDone()) {
                jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                addNotificationIfSpecificationHasDestination(Notification.Type.JOB_COMPLETED, jobEntity);
                logTimerMessage(jobEntity);
            }

            jobStoreRepository.flushEntityManager();
            jobStoreRepository.refreshFromDatabase(jobEntity);

            return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
        } else {
            final String errMsg = String.format("Chunk[%d,%d] contains illegal number of items %d when %d expected",
                    chunk.getJobId(), chunk.getChunkId(), chunk.size(), chunkEntity.getNumberOfItems());
            final JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, errMsg, null);
            throw new InvalidInputException(errMsg, jobError);
        }
    }

    /**
     * Sets a workflow note on a job
     * @param workflowNote the note to attach
     * @param jobId identifying the job
     * @return information snapshot of updated job
     * @throws JobStoreException if referenced job entity was not found
     */
    @Stopwatch
    public JobInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId) throws JobStoreException {
        final JobEntity jobEntity = jobStoreRepository.setJobEntityWorkFlowNote(workflowNote, jobId);
        return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
    }

    /**
     * Sets a workflow note on an item
     * @param workflowNote the note to attach
     * @param jobId identifying the job
     * @param chunkId identifying the chunk
     * @param itemId identifying the item
     * @return information snapshot of updated item
     * @throws JobStoreException if referenced item entity was not found
     */
    @Stopwatch
    public ItemInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId, int chunkId, short itemId) throws JobStoreException {
        final ItemEntity itemEntity = jobStoreRepository.setItemEntityWorkFlowNote(workflowNote, jobId, chunkId, itemId);
        return itemEntity.toItemInfoSnapshot();
    }

    // Method is package-private for unit testing purposes
    long getByteSizeOrThrow(String fileId) throws JobStoreException {
        try {
            return fileStoreServiceConnectorBean.getConnector().getByteSize(fileId);
        } catch(FileStoreServiceConnectorException ex) {
            LOGGER.warn("Could not retrieve byte size for file with id: {}", fileId);
            throw new JobStoreException("Could not retrieve byte size", ex);
        }
    }

    /**
     * Method is package-private for unit testing purposes
     * Method compares bytes size of a file with the byte size of
     * the data partitioner used when adding a job to the job store
     *
     * @param fileId file id
     * @param dataPartitioner containing the input steam
     * @throws IOException if the byte size differs
     * @throws JobStoreException if the byte size could not be retrieved,
     * InvalidInputException if the file store service URI was invalid
     */
    void compareByteSize(String fileId, DataPartitioner dataPartitioner)
            throws IOException, JobStoreException {
        long jobByteSize = dataPartitioner.getBytesRead();
        if (jobByteSize == DataPartitioner.NO_BYTE_COUNT_AVAILABLE) {
            return;
        }
        
        long fileByteSize = getByteSizeOrThrow(fileId);

        // Byte size reported by the file-store might be wrong
        // if the file uses gzip compression and originally
        // is larger than four GiB.
        if (jobByteSize > fileByteSize
                && (jobByteSize - fileByteSize) % FOUR_GIBIBYTE == 0) {
            // Since the reported size plus a multiple of four GiB
            // matched the number of bytes read we'll assume everything
            // is ok.
            return;
        }
        if (fileByteSize != jobByteSize) {
            throw new IOException(String.format(
                    "Error reading data file {%s}. DataPartitioner.byteSize was: %s. " +
                            "FileStore.byteSize was: %s",
                    fileId, jobByteSize, fileByteSize));
        }
    }

    private State updateChunkEntityState(ChunkEntity chunkEntity, StateChange stateChange) {
        final State chunkState = new State(chunkEntity.getState());
        chunkState.updateState(stateChange);
        chunkEntity.setState(chunkState);
        return chunkState;
    }

    private JobEntity abortJob(int jobId, String message, Throwable cause) {
        final JobEntity jobEntity = jobStoreRepository.getExclusiveAccessFor(JobEntity.class, jobId);
        return abortJob(jobEntity, Collections.singletonList(new Diagnostic(Diagnostic.Level.FATAL, message, cause)));
    }

    private JobEntity abortJob(JobEntity jobEntity, List<Diagnostic> diagnostics) {
        final State jobState = new State(jobEntity.getState());
        jobState.getDiagnostics().addAll(diagnostics);
        jobEntity.setState(jobState);
        jobEntity.setFatalError(true);
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        return jobEntity;
    }

    private void completeZeroChunkJob(JobEntity jobEntity) {
        StateChange processingCompleted = new StateChange().setPhase(State.Phase.PROCESSING).setEndDate(new Date());
        jobStoreRepository.updateJobEntityState(jobEntity, processingCompleted);

        StateChange deliveringCompleted = new StateChange().setPhase(State.Phase.DELIVERING).setEndDate(new Date());
        jobStoreRepository.updateJobEntityState(jobEntity, deliveringCompleted);

        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
    }

    private void addNotificationIfSpecificationHasDestination(Notification.Type type, JobEntity jobEntity) {
        if (jobEntity.getSpecification().hasNotificationDestination()) {
            jobNotificationRepository.addNotification(type, jobEntity);
        }
    }

    private void logTimerMessage(JobEntity jobEntity) {
        if (LOGGER.isInfoEnabled()) {
            final List<Object> logArguments = new ArrayList<>(6);
            logArguments.add(jobEntity.getId());
            logArguments.add(jobEntity.getNumberOfItems());
            logArguments.add(jobEntity.getNumberOfChunks());
            logArguments.add(jobEntity.getSpecification().getSubmitterId());
            logArguments.add(jobEntity.getSpecification().getDestination());
            String logPattern = "TIMER jobId={} numberOfItems={} numberOfChunks={} submitterNumber={} destination={}";

            // Time of creation will never be null. This check is only added for testing purpose.
            long timeOfCreation = 0;
            if (jobEntity.getTimeOfCreation() != null) {
                timeOfCreation = jobEntity.getTimeOfCreation().getTime();
                logPattern += " timeOfCreation={}";
                logArguments.add(timeOfCreation);
            }
            if (jobEntity.getTimeOfCompletion() != null) {
                final long timeOfCompletion = jobEntity.getTimeOfCompletion().getTime();
                logPattern += " timeOfCompletion={}";
                logArguments.add(timeOfCompletion);
                if (jobEntity.getNumberOfItems() > 0) {
                    logPattern += " avgDurationPerItemInMs={}";
                    logArguments.add((timeOfCompletion - timeOfCreation) / jobEntity.getNumberOfItems());
                }
            }
            LOGGER.info(logPattern, logArguments.toArray());
        }
    }
}
