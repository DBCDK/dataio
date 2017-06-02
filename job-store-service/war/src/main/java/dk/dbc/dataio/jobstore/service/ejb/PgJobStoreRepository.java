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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.digest.Md5;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ChunkListQuery;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowConverter;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemListQuery;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobListQuery;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkConverter;
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerResult;
import dk.dbc.dataio.jobstore.service.util.FlowTrimmer;
import dk.dbc.dataio.jobstore.service.util.IncludeFilter;
import dk.dbc.dataio.jobstore.service.util.JobExporter;
import dk.dbc.dataio.jobstore.service.util.TrackingIdGenerator;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static java.lang.String.format;

/**
 * This is an DAO Repository for internal use of the job-store-service hence package scoped methods.
 */
@Stateless
public class PgJobStoreRepository extends RepositoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreRepository.class);

    JSONBContext jsonbContext = new JSONBContext();

    public PgJobStoreRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * Creates job listing based on given criteria
     *
     * @param criteria job listing criteria
     * @return list of information snapshots of selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return new JobListQuery(entityManager).execute(criteria);
    }

    @Stopwatch
    public long countJobs(JobListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return new JobListQuery(entityManager).execute_count(criteria);
    }

    /**
     * Creates chunk collision detection element listing based on given criteria
     *
     * @param criteria chunk listing criteria
     * @return list of collision detection elements
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public List<CollisionDetectionElement> listChunksCollisionDetectionElements(ChunkListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        final List<ChunkEntity> chunkEntities = new ChunkListQuery(entityManager).execute(criteria);
        final List<CollisionDetectionElement> collisionDetectionElements = new ArrayList<>(chunkEntities.size());
        collisionDetectionElements.addAll(chunkEntities.stream().map(ChunkEntity::toCollisionDetectionElement).collect(Collectors.toList()));
        return collisionDetectionElements;
    }

    /**
     * Creates item listing based on given criteria
     *
     * @param criteria item listing criteria
     * @return list of information snapshots of selected items
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        final List<ItemEntity> itemEntities = new ItemListQuery(entityManager).execute(criteria);
        final List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(itemEntities.size());
        itemInfoSnapshots.addAll(itemEntities.stream().map(ItemEntity::toItemInfoSnapshot).collect(Collectors.toList()));
        return itemInfoSnapshots;
    }

    /**
     * Exports from a job all chunk items which have failed in a specific phase
     * @param jobId of the job
     * @param fromPhase specified phase
     * @param type of export
     * @param encodedAs specified encoding
     * @return byteArrayOutputStream containing the requested items.
     * @throws JobStoreException on general failure to write output stream
     */
    @Stopwatch
    public ByteArrayOutputStream itemsExport(int jobId, State.Phase fromPhase, ChunkItem.Type type, Charset encodedAs) throws JobStoreException {
        return new JobExporter(entityManager).exportFailedItemsContentStream(jobId, Collections.singletonList(fromPhase), type, encodedAs);
    }

    /**
     * @param criteria item listing criteria
     * @return the number of items located through the criteria
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public long countItems(ItemListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return new ItemListQuery(entityManager).execute_count(criteria);
    }

    /**
     * Creates new job entity and caches associated Flow and Sink as needed.
     * If any Diagnostic with level FATAL is located, the elements will not be cashed.
     * Instead timeOfCompletion is set on the jobEntity, to mark the job as finished as it will be unable to complete if added.
     * <p>
     * CAVEAT: Even though this method is publicly available it is <b>NOT</b>
     * intended for use outside of this class - accessibility is only so defined
     * to allow the method to be called internally as an EJB business method.
     * </p>
     *
     * @param addJobParam containing parameter abstraction for the parameters needed by PgJobStore.addJob() method.
     * @return created job entity (managed)
     * @throws JobStoreException if unable to cache associated flow or sink
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public JobEntity createJobEntity(AddJobParam addJobParam) throws JobStoreException {
        final JobEntity jobEntity = new JobEntity();
        final State jobState = new State();
        jobState.getDiagnostics().addAll(addJobParam.getDiagnostics());
        jobEntity.setEoj(addJobParam.getJobInputStream().getIsEndOfJob());
        jobEntity.setPartNumber(addJobParam.getJobInputStream().getPartNumber());
        jobEntity.setSpecification(addJobParam.getJobInputStream().getJobSpecification());
        jobEntity.setFlowStoreReferences(addJobParam.getFlowStoreReferences());
        jobEntity.setState(jobState);
        jobEntity.setPriority(addJobParam.getPriority());

        if (!jobState.fatalDiagnosticExists()) {
            jobState.getPhase(State.Phase.PARTITIONING).setBeginDate(new Date());
            try {
                String flowJson = jsonbContext.marshall(addJobParam.getFlow());
                if (addJobParam.getJobInputStream().getJobSpecification().getType() != JobSpecification.Type.ACCTEST) {
                    flowJson = new FlowTrimmer(jsonbContext).trim(flowJson);
                }
                jobEntity.setCachedFlow(cacheFlow(flowJson));
                jobEntity.setCachedSink(cacheSink(jsonbContext.marshall(addJobParam.getSink())));
            } catch (JSONBException e) {
                throw new JobStoreException("Exception caught during job-store operation", e);
            }
        } else {
            jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            jobEntity.setFatalError(true);
        }
        entityManager.persist(jobEntity);
        entityManager.flush();
        entityManager.refresh(jobEntity);
        return jobEntity;
    }

    /**
     * Creates new chunk and associated data item entities and updates the state of the containing job
     * <p>
     * CAVEAT: Even though this method is publicly available it is <b>NOT</b>
     * intended for use outside of this class - accessibility is only so defined
     * to allow the method to be called internally as an EJB business method.
     * </p>
     * @param submitterId                  submitter number
     * @param jobId                        id of job for which the chunk is to be created
     * @param chunkId                      id of the chunk to be created
     * @param maxChunkSize                 maximum number of items to be associated to the chunk
     * @param dataPartitioner              data partitioner used for item data extraction
     * @param sequenceAnalyserKeyGenerator sequence analyser key generator
     * @param dataFileId                   id of data file from where the items of the chunk originated
     * @param includeFilter                filter for item ids
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion
     * @throws JobStoreException on referenced entities not found
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ChunkEntity createChunkEntity(long submitterId, int jobId, int chunkId, short maxChunkSize,
            DataPartitioner dataPartitioner,
            SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator,
            String dataFileId, IncludeFilter includeFilter) throws JobStoreException {

        final ChunkEntity chunkEntity = persistChunk(jobId, chunkId, dataFileId);

        // create items
        final ChunkItemEntities chunkItemEntities = createChunkItemEntities(
            submitterId, jobId, chunkId, maxChunkSize, dataPartitioner, includeFilter);
        if (chunkItemEntities.size() > 0) {
            chunkEntity.setNumberOfItems(chunkItemEntities.size());
            chunkEntity.setSequenceAnalysisData(getSequenceAnalysisData(sequenceAnalyserKeyGenerator, chunkItemEntities));

            final State chunkState = chunkItemEntities.getChunkState();
            chunkEntity.setState(chunkState);
            if (chunkState.fatalDiagnosticExists()) {
                chunkEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            }

            // update job (with exclusive lock)
            final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
            jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
            jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
            jobEntity.setSkipped(jobEntity.getSkipped() + chunkItemEntities.getSkipped());
            updateJobEntityState(jobEntity, chunkItemEntities.chunkStateChange.setBeginDate(null).setEndDate(null));
        } else {
            entityManager.remove(chunkEntity);
            return null;
        }
        return chunkEntity;
    }

    /**
     * Creates new chunk Job Termination ChunkEntity and associated data item entities and updates the state of the containing job
     * <p>
     * CAVEAT: Even though this method is publicly available it is <b>NOT</b>
     * intended for use outside of this class - accessibility is only so defined
     * to allow the method to be called internally as an EJB business method.
     * </p>
     *
     * @param jobId      id of job for which the chunk is to be created
     * @param chunkId    id of the chunk to be created
     * @param dataFileId for fake chunk
     * @param itemStatus status for TICKLE_JOB_END item
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion*
     * @throws JobStoreException on referenced entities not found
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ChunkEntity createJobTerminationChunkEntity(
            int jobId,
            int chunkId,
            String dataFileId, ChunkItem.Status itemStatus)
            throws JobStoreException {

        final Date chunkBegin = new Date();

        // Create ChunkItemEntities
        short itemId=0;

        final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PARTITIONING);

        final ChunkItem chunkItem = new ChunkItem().withId( itemId )
                                        .withStatus(itemStatus)
                                        .withType(ChunkItem.Type.TICKLE_JOB_END)
                .withData("Tickle Job Termination Item")
                .withTrackingId(format("TickleEndItem for Job %d", jobId));


        final State itemState = new State();
        
        itemState.updateState(new StateChange().setPhase(State.Phase.PARTITIONING).setBeginDate(chunkBegin).setEndDate(new Date()).setSucceeded(1));
        itemState.updateState(new StateChange().setPhase(State.Phase.PROCESSING).setBeginDate(chunkBegin).setEndDate(new Date()).setSucceeded(1));


        RecordInfo recordInfo = new RecordInfo("End Item");

        final ItemEntity itemEntity= persistItem(jobId, chunkId, itemId, itemState, chunkItem, recordInfo);
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PROCESSING);
        itemEntity.setState( itemState );
        itemEntity.setProcessingOutcome( chunkItem );

        chunkItemEntities.entities.add(itemEntity);
        // ChunkItem Entities created

        // Items were created, so now create the chunk to which they belong
        final StateChange chunkStateChange = chunkItemEntities.chunkStateChange.setBeginDate(chunkBegin);
        SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(new HashSet<>());

        final State chunkState = new State();

        Date now=new Date();
        chunkState.updateState( new StateChange().setPhase(State.Phase.PARTITIONING).setBeginDate(chunkBegin).setEndDate(now).setSucceeded(1));
        chunkState.updateState( new StateChange().setPhase(State.Phase.PROCESSING).setBeginDate(now).setEndDate(now).setSucceeded(1));
        
        ChunkEntity chunkEntity = initializeChunkEntityAndSetValues(jobId, chunkId, dataFileId, chunkItemEntities, sequenceAnalysisData, chunkState);

        entityManager.persist(chunkEntity);
        entityManager.flush();
        entityManager.refresh(chunkEntity);

        // update job (with exclusive lock)
        final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
        jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
        jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
        updateJobEntityState(jobEntity, chunkStateChange.setBeginDate(null).setEndDate(null));
        entityManager.flush();

        return chunkEntity;
    }

    /**
     * @param entityClass class of the Entity
     * @param primaryKey the primary key
     * @param <T> the type
     * @return locked entity
     */
    public <T> T getExclusiveAccessFor(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * @param jobEntity Job Entity
     * @param stateChange changed state of the Job
     * @return the updated state
     */
    public State updateJobEntityState(JobEntity jobEntity, StateChange stateChange) {
        final State jobState = new State(jobEntity.getState());
        jobState.updateState(stateChange);
        jobEntity.setState(jobState);
        return jobState;
    }

    /**
     * sets a workflow note on an existing job. Any workflow previously added will be wiped in the process
     *
     * @param workflowNote the note to set
     * @param jobId of the job to which a workflow note should be attached.
     * @return the updated jobEntity
     * @throws JobStoreException if unable to find referenced job entity
     */
    public JobEntity setJobEntityWorkFlowNote(WorkflowNote workflowNote, int jobId) throws JobStoreException {
        final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
        if (jobEntity == null) {
            throw new JobStoreException(format("JobEntity.%s could not be found", jobId));
        }
        jobEntity.setWorkflowNote(workflowNote);
        return jobEntity;
    }

    /**
     * sets a workflow note on an existing item. Any workflow previously added will be wiped in the process
     *
     * @param workflowNote the note to set
     * @param jobId of the referenced job
     * @param chunkId of the referenced chunk
     * @param itemId of the item to which a workflow note should be attached.
     * @return the updated itemEntity
     * @throws JobStoreException if unable to find referenced item entity
     */
    public ItemEntity setItemEntityWorkFlowNote(WorkflowNote workflowNote, int jobId, int chunkId, short itemId) throws JobStoreException {
        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = getExclusiveAccessFor(ItemEntity.class, key);
        if (itemEntity == null) {
            throw new JobStoreException(format("ItemEntity.key{jobId: %s, chunkId: %s, itemId: %s} could not be found", jobId, chunkId, itemId));
        }
        itemEntity.setWorkflowNote(workflowNote);
        return itemEntity;
    }

    /**
     * Retrieves the cached flow from the specified job entity.
     *
     * @param jobId of job to bundle resources for
     * @return resource bundle
     * @throws InvalidInputException on failure to retrieve job
     * @throws NullPointerException on null valued input when creating new resource bundle
     */
    @Stopwatch
    public Flow getCachedFlow(int jobId) throws JobStoreException, NullPointerException {
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
        if (jobEntity == null) {
            throwInvalidInputException(format("JobEntity.%d could not be found", jobId), JobError.Code.INVALID_JOB_IDENTIFIER);
        }
        return jobEntity.getCachedFlow().getFlow();
    }

    /**
     * @param type type of requested chunk
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk
     * @return chunk representation for given chunk ID, job ID and type or
     * null if no item entities could be found
     * @throws NullPointerException if given null-valued type or if any of
     * underlying item entities contains no data for the corresponding phase
     */
    @Stopwatch
    public Chunk getChunk(Chunk.Type type, int jobId, int chunkId) throws NullPointerException {
        final Profiler profiler=new Profiler("pgJobStoreRepository.getChunk");
        try {
            final State.Phase phase = chunkTypeToStatePhase(InvariantUtil.checkNotNullOrThrow(type, "type"));
            final ItemListCriteria criteria = new ItemListCriteria()
                    .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId))
                    .and(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, chunkId))
                    .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));

            profiler.start("execute Query");
            final List<ItemEntity> itemEntities = new ItemListQuery(entityManager).execute(criteria);
            profiler.stop();
            if (itemEntities.size() > 0) {
                profiler.start("Loop itemEntities");
                final Chunk chunk = new Chunk(jobId, chunkId, type);
                int i=0;
                for (ItemEntity itemEntity : itemEntities) {
                    if (PROCESSED == type) {
                        // Special case for chunks containing 'next' items - only relevant in phase PROCESSED
                        chunk.insertItem(itemEntity.getProcessingOutcome(), itemEntity.getNextProcessingOutcome());
                    } else {
                        chunk.insertItem(itemEntity.getChunkItemForPhase(phase));
                    }
                    ++i;
                }
                chunk.setEncoding(StandardCharsets.UTF_8); // TODO: 15/01/16 This is a temporary solution that should be removed once encoding is removed from Chunk
                return chunk;
            }
            return null;
        } finally {
            LOGGER.info("pgJobStoreRepository.getChunk timings:\n"+profiler.toString());
        }
    }

    @Stopwatch
    public ChunkItem getChunkItemForPhase(int jobId, int chunkId, short itemId, State.Phase phase) throws InvalidInputException {
        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, key);
        if (itemEntity == null) {
            throwInvalidInputException(format("ItemEntity.Key{jobId:%d, chunkId:%d, itemId:%d} could not be found", jobId, chunkId, itemId), JobError.Code.INVALID_ITEM_IDENTIFIER);
        }
        switch(phase) {
            case PARTITIONING:  return itemEntity.getPartitioningOutcome();
            case PROCESSING:    return itemEntity.getProcessingOutcome();
            default:            return itemEntity.getDeliveringOutcome();
        }
    }

    /**
     * Retrieves next processing outcome as chunk item
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk containing item
     * @param itemId id of the item
     *
     * @return next processing outcome
     * @throws InvalidInputException if unable to find referenced item
     */
    @Stopwatch
    public ChunkItem getNextProcessingOutcome(int jobId, int chunkId, short itemId) throws InvalidInputException {
        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, key);
        if (itemEntity == null) {
            throwInvalidInputException(format("ItemEntity.Key{jobId:%d, chunkId:%d, itemId:%d} could not be found", jobId, chunkId, itemId), JobError.Code.INVALID_ITEM_IDENTIFIER);
        }
        return itemEntity.getNextProcessingOutcome();
    }

    /**
     * Updates item entities for given chunk
     * @param chunk chunk
     * @return item entities compound object
     * @throws DuplicateChunkException if attempting to update already existing chunk
     * @throws InvalidInputException if unable to find referenced items or if chunk belongs to PARTITIONING
     * phase
     * @throws JobStoreException Job Store Exception
     */
    @Stopwatch
    public ChunkItemEntities updateChunkItemEntities(Chunk chunk) throws JobStoreException {
        Date nextItemBegin = new Date();

        final State.Phase phase = chunkTypeToStatePhase(chunk.getType());
        final PgJobStoreRepository.ChunkItemEntities chunkItemEntities = new PgJobStoreRepository.ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(phase);

        final Iterator<ChunkItem> nextIterator = chunk.nextIterator();
        try {
            for (ChunkItem chunkItem : chunk) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                LOGGER.info("updateChunkItemEntities: updating {} chunk item {}/{}/{}",
                        chunk.getType(), chunk.getJobId(), chunk.getChunkId(), chunkItem.getId());
                final ItemEntity.Key itemKey = new ItemEntity.Key((int) chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId());
                final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
                if (itemEntity == null) {
                    throwInvalidInputException(format("ItemEntity.%s could not be found", itemKey), JobError.Code.INVALID_ITEM_IDENTIFIER);
                }

                if (itemEntity.getState().phaseIsDone(phase)) {
                    throwDuplicateChunkException(format("Aborted attempt to add item %s to already finished %s phase", itemEntity.getKey(), phase), JobError.Code.ILLEGAL_CHUNK);
                }

                chunkItemEntities.entities.add(itemEntity);

                final StateChange itemStateChange = new StateChange()
                        .setPhase(phase)
                        .setBeginDate(nextItemBegin)                                            // ToDo: Chunk type must contain beginDate
                        .setEndDate(new Date());                                                // ToDo: Chunk type must contain endDate

                setOutcomeOnItemEntityFromPhase(chunk, phase, itemEntity, chunkItem);
                if (nextIterator.hasNext()) {
                    itemEntity.setNextProcessingOutcome(nextIterator.next());
                }

                setItemStateOnChunkItemFromStatus(chunkItemEntities, chunkItem, itemStateChange);

                final State itemState = updateItemEntityState(itemEntity, itemStateChange);
                if (itemState.allPhasesAreDone()) {
                    itemEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                }
                nextItemBegin = new Date();
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return chunkItemEntities;
    }

    /**
     * Adds Flow instance to job-store cache if not already cached
     * @param flowJson Flow document to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued flowJson
     * @throws IllegalArgumentException if given empty-valued flowJson
     * @throws IllegalStateException if unable to create checksum digest
     * entity object to JSON
     */
    @Stopwatch
    public FlowCacheEntity cacheFlow(String flowJson) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowJson, "flow");
        final Query storedProcedure = entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE);
        storedProcedure.setParameter("checksum", Md5.asHex(flowJson.getBytes(StandardCharsets.UTF_8)));
        storedProcedure.setParameter("flow", new FlowConverter().convertToDatabaseColumn(flowJson));
        return (FlowCacheEntity) storedProcedure.getSingleResult();
    }

    /**
     * Adds Sink instance to job-store cache if not already cached
     * @param sinkJson Sink document to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued sinkJson
     * @throws IllegalArgumentException if given empty-valued sinkJson
     * @throws IllegalStateException if unable to create checksum digest
     * entity object to JSON
     */
    @Stopwatch
    public SinkCacheEntity cacheSink(String sinkJson) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkJson, "sink");
        final Query storedProcedure = entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE);
        storedProcedure.setParameter("checksum", Md5.asHex(sinkJson.getBytes(StandardCharsets.UTF_8)));
        storedProcedure.setParameter("sink", new SinkConverter().convertToDatabaseColumn(sinkJson));
        return (SinkCacheEntity) storedProcedure.getSingleResult();
    }

    /**
     * Creates item entities for given chunk using data extracted via given data partitioner
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk for which items are to be created
     * @param maxChunkSize maximum number of items to be associated to the chunk
     * @param dataPartitioner data partitioner used for item data extraction
     * @return item entities compound object
     */
    @Stopwatch
    ChunkItemEntities createChunkItemEntities(long submitterId, int jobId,
            int chunkId, short maxChunkSize, DataPartitioner dataPartitioner, IncludeFilter includeFilter) {
        Date nextItemBegin = new Date();
        short itemCounter = 0;
        includeFilter.withVirtualItemCounter(0);
        final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PARTITIONING);
        try {
            /* For each data entry extracted create the containing item entity.
               In case a DataException is thrown by the extraction process a item entity
               is still created but with a serialized JobError as payload instead.
             */

            final SinkContent.SequenceAnalysisOption sequenceAnalysisOption = getSequenceAnalysisOption(jobId);
            for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
                if (dataPartitionerResult.isEmpty()) {
                    continue;
                }
                if(!includeFilter.include(includeFilter.incrementVirtualCounter())) {
                    chunkItemEntities.incrementSkipped();
                    continue;
                }

                final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
                String trackingId = chunkItem.getTrackingId();
                if (trackingId == null || trackingId.trim().isEmpty()) {
                    // Generate dataio specific tracking id
                    RecordInfo recordInfo = dataPartitionerResult.getRecordInfo();
                    if(recordInfo instanceof MarcRecordInfo) {
                        String recordId = recordInfo.getId();
                        trackingId = TrackingIdGenerator.getTrackingId(
                            submitterId, recordId, jobId, chunkId, itemCounter);
                        chunkItem.withTrackingId(trackingId);
                    } else {
                        trackingId = TrackingIdGenerator.getTrackingId(jobId,
                            chunkId, itemCounter);
                        chunkItem.withTrackingId(trackingId);
                    }
                }
                DBCTrackedLogContext.setTrackingId(trackingId);
                LOGGER.info("Creating chunk item {}/{}/{}", jobId, chunkId, itemCounter);

                StateChange stateChange = new StateChange()
                        .setPhase(State.Phase.PARTITIONING)
                        .setBeginDate(nextItemBegin)
                        .setEndDate(new Date());

                setItemStateOnChunkItemFromStatus(chunkItemEntities, chunkItem, stateChange);

                final State itemState = new State();
                itemState.updateState(stateChange);

                chunkItem.withId(itemCounter);
                chunkItemEntities.entities.add(persistItem(jobId, chunkId, itemCounter++, itemState, chunkItem, dataPartitionerResult.getRecordInfo()));
                if (dataPartitionerResult.getRecordInfo() != null) {
                    chunkItemEntities.keys.addAll(dataPartitionerResult.getRecordInfo().getKeys(sequenceAnalysisOption));
                }

                if (itemCounter == maxChunkSize) {
                    break;
                }
                nextItemBegin = new Date();
            }
        } catch (PrematureEndOfDataException e) {
            throw e;
        } catch (RuntimeException e) {
            LOGGER.warn("Unrecoverable exception caught during job partitioning of job {}", jobId, e);
            final Diagnostic diagnostic = ObjectFactory.buildFatalDiagnostic(
                    format("Unable to complete partitioning at chunk %d item %d: %s",
                            chunkId, itemCounter, e.getMessage()), e);

            final StateChange stateChange = new StateChange()
                    .setPhase(State.Phase.PARTITIONING)
                    .setFailed(1)
                    .setBeginDate(nextItemBegin)
                    .setEndDate(new Date());
            final State itemState = new State();
            itemState.getDiagnostics().add(diagnostic);
            itemState.updateState(stateChange);

            chunkItemEntities.entities.add(persistItem(jobId, chunkId, itemCounter, itemState, null, null));
            chunkItemEntities.chunkStateChange.incFailed(1);
        } finally {
            DBCTrackedLogContext.remove();
        }
        return chunkItemEntities;
    }

    /**
     * Updates the job with item information before closing it
     * @param jobId of the job to preview
     * @param dataPartitioner data partitioner used for item data extraction
     * @return job preview
     */
    public JobEntity preview(int jobId, DataPartitioner dataPartitioner) {
        Date beginDate = new Date();
        int failed = 0;
        int succeeded = 0;
        try {
            for (DataPartitionerResult ignored : dataPartitioner) {
                succeeded ++;
            }
        } catch (PrematureEndOfDataException e) {
            throw e;
        } catch (RuntimeException e) {
            failed ++;
        }
        StateChange stateChange = new StateChange();
        stateChange.setPhase(State.Phase.PARTITIONING);
        stateChange.setBeginDate(beginDate);
        stateChange.setEndDate(new Date());
        stateChange.setFailed(failed);
        stateChange.setSucceeded(succeeded);

        final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
        final State jobState = new State(jobEntity.getState());
        jobState.updateState(stateChange);
        jobEntity.setState(jobState);
        jobEntity.setNumberOfItems(succeeded + failed);
        return jobEntity;
    }

    /**
     * Creates new item entity with data from the partitioning phase
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk for which items are to be created
     * @param itemId id of item
     * @param state initial state of item
     * @param chunkItem result of the partitioning phase
     * @param recordInfo recordInfo for Item
     * @return created item entity (managed)
     */
    private ItemEntity persistItem(int jobId, int chunkId, short itemId, State state, ChunkItem chunkItem, RecordInfo recordInfo) {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(new ItemEntity.Key(jobId, chunkId, itemId));
        itemEntity.setState(state);
        itemEntity.setPartitioningOutcome(chunkItem);
        itemEntity.setRecordInfo(recordInfo);
        entityManager.persist(itemEntity);
        return itemEntity;
    }

    private ChunkEntity persistChunk(int jobId, int chunkId, String dataFileId) {
        final ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(new ChunkEntity.Key(chunkId, jobId));
        chunkEntity.setDataFileId(dataFileId);
        chunkEntity.setNumberOfItems((short) 0);
        chunkEntity.setState(new State());
        chunkEntity.setSequenceAnalysisData(new SequenceAnalysisData(Collections.emptySet()));
        entityManager.persist(chunkEntity);
        return chunkEntity;
    }

    // // TODO: 4/4/17 deprecate this method - use persistChunk() + local changes instead
    private ChunkEntity initializeChunkEntityAndSetValues(int jobId, int chunkId, String dataFileId, ChunkItemEntities chunkItemEntities, SequenceAnalysisData sequenceAnalysisData, State chunkState) {
        ChunkEntity chunkEntity;
        chunkEntity = new ChunkEntity();
        chunkEntity.setKey(new ChunkEntity.Key(chunkId, jobId));
        chunkEntity.setNumberOfItems(chunkItemEntities.size());
        chunkEntity.setDataFileId(dataFileId);
        chunkEntity.setSequenceAnalysisData(sequenceAnalysisData);
        chunkEntity.setState(chunkState);
        if (chunkState.fatalDiagnosticExists()) {
            chunkEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        }
        return chunkEntity;
    }

    private void setItemStateOnChunkItemFromStatus(PgJobStoreRepository.ChunkItemEntities chunkItemEntities, ChunkItem chunkItem, StateChange itemStateChange) {
        switch (chunkItem.getStatus()) {
            case FAILURE:
                itemStateChange.setFailed(1);
                chunkItemEntities.chunkStateChange.incFailed(1);
                break;
            case IGNORE:
                itemStateChange.setIgnored(1);
                chunkItemEntities.chunkStateChange.incIgnored(1);
                break;
            case SUCCESS:
                itemStateChange.setSucceeded(1);
                chunkItemEntities.chunkStateChange.incSucceeded(1);
                break;
        }
    }
    private State updateItemEntityState(ItemEntity itemEntity, StateChange stateChange) {
        final State itemState = new State(itemEntity.getState());
        itemState.updateState(stateChange);
        itemEntity.setState(itemState);
        return itemState;
    }
    private void setOutcomeOnItemEntityFromPhase(Chunk chunk, State.Phase phase, ItemEntity itemEntity, ChunkItem chunkItem) throws InvalidInputException {
        switch (phase) {
            case PROCESSING: itemEntity.setProcessingOutcome(chunkItem);
                break;
            case DELIVERING: itemEntity.setDeliveringOutcome(chunkItem);
                break;
            case PARTITIONING:
                throwInvalidInputException(format("Trying to add items to %s phase of Chunk[%d,%d]", phase, chunk.getJobId(), chunk.getChunkId()), JobError.Code.ILLEGAL_CHUNK);
        }
    }

    private SequenceAnalysisData getSequenceAnalysisData(SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, ChunkItemEntities chunkItemEntities) {
        return new SequenceAnalysisData(sequenceAnalyserKeyGenerator.generateKeys(chunkItemEntities.keys));
    }

    private void throwInvalidInputException(String errMsg, JobError.Code jobErrorCode) throws InvalidInputException {
        final JobError jobError = new JobError(jobErrorCode, errMsg, JobError.NO_STACKTRACE);
        throw new InvalidInputException(errMsg, jobError);
    }

    private State.Phase chunkTypeToStatePhase(Chunk.Type chunkType) {
        switch (chunkType) {
            case PARTITIONED: return State.Phase.PARTITIONING;
            case PROCESSED:   return State.Phase.PROCESSING;
            case DELIVERED:   return State.Phase.DELIVERING;
            default: throw new IllegalStateException(format("Unknown type: '%s'", chunkType));
        }
    }
    private void throwDuplicateChunkException(String errMsg, JobError.Code jobErrorCode) throws DuplicateChunkException {
        final JobError jobError = new JobError(jobErrorCode, errMsg, JobError.NO_STACKTRACE);
        throw new DuplicateChunkException(errMsg, jobError);
    }

    // EJB specification dictates Public across EJB's.
    public static class  ChunkItemEntities {
        public final List<ItemEntity> entities;
        public final StateChange chunkStateChange;
        public final List<String> keys;
        public int skipped;

        public ChunkItemEntities() {
            entities = new ArrayList<>();
            chunkStateChange = new StateChange();
            chunkStateChange.setBeginDate(new Date());
            keys = new ArrayList<>();
            skipped = 0;
        }

        public short size() {
            return (short) entities.size();
        }

        public State getChunkState() {
            final State chunkState = createChunkStateFromItemDiagnostics();
            chunkState.updateState(chunkStateChange.setEndDate(new Date()));
            return chunkState;
        }

        private State createChunkStateFromItemDiagnostics() {
            final State chunkState = new State();
            for (final ItemEntity itemEntity : entities) {
                chunkState.getDiagnostics().addAll(itemEntity.getState().getDiagnostics());
            }
            return chunkState;
        }

        public int getSkipped() {
            return skipped;
        }

        public void incrementSkipped() {
            this.skipped++;
        }
    }

    private SinkContent.SequenceAnalysisOption getSequenceAnalysisOption(int jobId) {
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
        return jobEntity.getCachedSink().getSink().getContent().getSequenceAnalysisOption();
    }
}
