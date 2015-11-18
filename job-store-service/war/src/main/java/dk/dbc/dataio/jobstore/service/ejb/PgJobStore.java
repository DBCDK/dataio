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
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the job-store database through persistence layer
 */
@Stateless
public class PgJobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStore.class);

    /* These instances are not private otherwise they were not accessible from automatic test */
    @EJB JobSchedulerBean jobSchedulerBean;
    @EJB FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB PgJobStoreRepository jobStoreRepository;
    @EJB JobQueueRepository jobQueueRepository;
    @EJB JobNotificationRepository jobNotificationRepository;

    @Resource SessionContext sessionContext;

    @Stopwatch
    public String testMe() {
        return "Det virker fantastisk.";
    }

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

    /**
     * Adds new job job, chunk and item entities in the underlying data store from given job input stream
     * @param addJobParam containing the elements required to create a new job as well as a list of Diagnostics.
     *                    If the list contains any diagnostic with level FATAL, the job will be marked as finished
     *                    before partitioning is attempted.
     * @return information snapshot of added job
     * @throws NullPointerException if given any null-valued argument
     * @throws JobStoreException on failure to add job
     */
    @Stopwatch
    public JobInfoSnapshot addJob(AddJobParam addJobParam) throws JobStoreException {
        // Creates job entity in its own transactional scope to enable external visibility
        JobEntity jobEntity = jobStoreRepository.createJobEntity(addJobParam);
        LOGGER.info("Adding job with job ID: {}", jobEntity.getId());

        addNotificationIfSpecificationHasDestination(JobNotification.Type.JOB_CREATED, jobEntity);

        if(!jobEntity.hasFatalError()) {
            getProxyToSelf().handlePartitioningAsynchronously(
                    new PartitioningParam(
                            jobEntity,
                            fileStoreServiceConnectorBean.getConnector(),
                            addJobParam.getFlowBinder().getContent().getSequenceAnalysis(),
                            addJobParam.getFlowBinder().getContent().getRecordSplitter()));
        }

        return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
    }

    private PgJobStore getProxyToSelf() {
        return sessionContext.getBusinessObject(PgJobStore.class);
    }

    /**
     * This method is a public wrapper to support asynchronous behavior
     *
     * @param partitioningParam     see description in above addJob method
     * @throws JobStoreException    see description in above addJob method
     */
    @Asynchronous
    public void handlePartitioningAsynchronously(PartitioningParam partitioningParam) throws JobStoreException {
        handlePartitioning(partitioningParam);
    }

    /**
     * This method has package scope so testing is possible
     * @param partitioningParam     see description in above addJob method
     * @return JobInfoSnapshot      information snapshot of added job
     * @throws JobStoreException    see description in above addJob method
     */
    JobInfoSnapshot handlePartitioning(PartitioningParam partitioningParam) throws JobStoreException {

        JobEntity jobEntity = partitioningParam.getJobEntity();
        boolean sinkAvailable = true;

        if (!jobEntity.hasFatalError()) {

            sinkAvailable = isSinkAvailableAndAddJobToJobQueue(
                    jobEntity,
                    jobEntity.getCachedSink().getSink(),
                    partitioningParam.getDoSequenceAnalysis(),
                    partitioningParam.getRecordSplitterType());
        }

        final List<Diagnostic> abortDiagnostics = new ArrayList<>(0);


        try {  // This try have a 'finally' which ensures that the file is closed even if an unhandled exception is thrown!
            if (sinkAvailable) {

                // Continue only if no fatal error has occurred (all required remote entities could be located).
                if (!jobEntity.hasFatalError()) {

                    LOGGER.info("Start partitioning...");
                    doPartitioningToChunksAndItems(partitioningParam, jobEntity.getId(), abortDiagnostics);

                    // Job partitioning is now done - signalled by setting the endDate property of the PARTITIONING phase.
                    final StateChange jobStateChange = new StateChange().setPhase(State.Phase.PARTITIONING).setEndDate(new Date());

                    jobEntity = jobStoreRepository.getExclusiveAccessFor(JobEntity.class, jobEntity.getId());
                    jobStoreRepository.updateJobEntityState(jobEntity, jobStateChange);
                    if (!abortDiagnostics.isEmpty()) {
                        abortJob(jobEntity, abortDiagnostics);
                    }
                }

                /*
                    Important that this is done after partitioning is done otherwise DataPartitioner is not updated with file info - hence filesize is 0.
                    & it is also important that catching all possible exceptions and set diagnostics otherwise the transaction is rolled back hence state will be incorrect!
                */
                if (!jobEntity.hasFatalError()) {
                    try {
                        compareByteSize(partitioningParam.getDataFileId(), partitioningParam.getDataPartitioner());
                    } catch (Exception exception) {
                        final String diagnosticMessageOnJobLevel = String.format("Partitioning succeeded but validation 'compareByteSize' failed: %s", exception.getMessage());
                        final Diagnostic diagnostic = new Diagnostic(FATAL, diagnosticMessageOnJobLevel, exception);

                        abortDiagnostics.add(diagnostic);
                        if (!abortDiagnostics.isEmpty()) {
                            abortJob(jobEntity, abortDiagnostics);
                        }
                    }
                }

                jobQueueRepository.removeJobFromJobQueueIfExists(jobEntity);

            } else {
                LOGGER.info("Skip partitioning...");
            }
        } finally {

            partitioningParam.closeDataFile();
        }

        jobStoreRepository.flushEntityManager();
        logTimerMessage(jobEntity);

        return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
    }

    // Important that the Sink ID is the real Sink's ID and not the cached ones because the Cache can hold the same Sink in different versions.
    private boolean isSinkAvailableAndAddJobToJobQueue(JobEntity job, Sink sink, boolean doSequenceAnalysis, RecordSplitterConstants.RecordSplitter recordSplitterType) {

        final boolean SINK_OCCUPIED = false;
        final boolean SINK_AVAILABLE = true;

        boolean sinkOccupied = this.jobQueueRepository.addJobToJobQueueInDatabase(sink.getId(), job, doSequenceAnalysis, recordSplitterType);

        LOGGER.info(String.format("Sink (ID: %s) - occupied: %s", sink.getId(), sinkOccupied));
        if(sinkOccupied) {
            return SINK_OCCUPIED;
        } else {
            return SINK_AVAILABLE;
        }
    }

    void doPartitioningToChunksAndItems(
            PartitioningParam partitioningParam,
            int jobId,
            List<Diagnostic> abortDiagnostics) throws JobStoreException {

        final ChunkEntity NO_MORE_CHUNKS = null;
        final short maxChunkSize = 10;
        int chunkId = 0;
        ChunkEntity chunkEntity;
        do {
            // Creates each chunk entity (and associated item entities) in its own
            // transactional scope to enable external visibility of job creation progress
            chunkEntity = jobStoreRepository.createChunkEntity(
                    jobId,
                    chunkId++,
                    maxChunkSize,
                    partitioningParam.getDataPartitioner(),
                    partitioningParam.getSequenceAnalyserKeyGenerator(),
                    partitioningParam.getJobEntity().getSpecification().getDataFile());

            if (chunkEntity == NO_MORE_CHUNKS) {
                break;
            }
            if (chunkEntity.getState().fatalDiagnosticExists()) {
                // Partitioning resulted in unrecoverable error - set diagnostic to force job abortion
                abortDiagnostics.addAll(chunkEntity.getState().getDiagnostics());
                break;
            }
            jobSchedulerBean.scheduleChunk(chunkEntity.toCollisionDetectionElement(), partitioningParam.getJobEntity().getCachedSink().getSink());
        } while (true);
    }

    /**
     * Adds chunk by updating existing items, chunk and job entities in the underlying data store.
     * @param chunk external chunk
     * @return information snapshot of updated job
     * @throws NullPointerException if given null-valued chunk argument
     * @throws DuplicateChunkException if attempting to update already existing chunk
     * @throws InvalidInputException if unable to find referenced items, if external chunk belongs to PARTITIONING phase
     * or if external chunk contains a number of items not matching that of the internal chunk entity
     * @throws JobStoreException if unable to find referenced chunk or job entities
     */
    @Stopwatch
    public JobInfoSnapshot addChunk(ExternalChunk chunk) throws NullPointerException, JobStoreException {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        LOGGER.info("Adding chunk[{},{}]", chunk.getJobId(), chunk.getChunkId());

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
                jobSchedulerBean.releaseChunk((ChunkIdentifier) chunkEntity.toCollisionDetectionElement().getIdentifier());
            }

            // update job
            final JobEntity jobEntity = jobStoreRepository.getExclusiveAccessFor(JobEntity.class, chunkEntity.getKey().getJobId());
            if (jobEntity == null) {
                throw new JobStoreException(String.format("JobEntity.%d could not be found", chunkEntity.getKey().getJobId()));
            }
            final State jobState = jobStoreRepository.updateJobEntityState(jobEntity, chunkStateChange.setBeginDate(null).setEndDate(null));
            if (jobState.allPhasesAreDone()) {
                jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                addNotificationIfSpecificationHasDestination(JobNotification.Type.JOB_COMPLETED, jobEntity);
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

    /*
     * package private methods
     */

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
     * Method compares bytes size of a file with the byte size of the data partitioner used when adding a job to the job store
     *
     * @param fileId file id
     * @param dataPartitioner containing the input steam
     * @throws IOException if the byte size differs
     * @throws JobStoreException if the byte size could not be retrieved, InvalidInputException if the file store service URI was invalid
     */
    void compareByteSize(String fileId, DataPartitionerFactory.DataPartitioner dataPartitioner) throws IOException, JobStoreException {
        long fileByteSize = getByteSizeOrThrow(fileId);
        long jobByteSize = dataPartitioner.getBytesRead();

        if(fileByteSize != jobByteSize){
            throw new IOException(String.format(
                    "Error reading data file {%s}. DataPartitioner.byteSize was: %s. FileStore.byteSize was: %s",
                    fileId, jobByteSize, fileByteSize));
        }
    }

    private State updateChunkEntityState(ChunkEntity chunkEntity, StateChange stateChange) {
        final State chunkState = new State(chunkEntity.getState());
        chunkState.updateState(stateChange);
        chunkEntity.setState(chunkState);
        return chunkState;
    }

    private JobEntity abortJob(JobEntity jobEntity, List<Diagnostic> diagnostics) {
        final State jobState = new State(jobEntity.getState());
        jobState.getDiagnostics().addAll(diagnostics);
        jobEntity.setState(jobState);
        jobEntity.setFatalError(true);
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        return jobEntity;
    }

    private void addNotificationIfSpecificationHasDestination(JobNotification.Type type, JobEntity jobEntity) {
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