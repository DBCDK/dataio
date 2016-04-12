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
import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
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
import dk.dbc.dataio.jobstore.service.util.ItemInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.service.util.JobExporter;
import dk.dbc.dataio.jobstore.service.util.TrackingIdGenerator;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
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
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ThomasBerg @LundOgBendsen on 02/09/15.
 *
 * This is an DAO Repository for internal use of the job-store-service hence package scoped methods.
 */
@Stateless
public class PgJobStoreRepository extends RepositoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreRepository.class);

    JSONBContext jsonbContext = new JSONBContext();

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
        itemInfoSnapshots.addAll(itemEntities.stream().map(ItemInfoSnapshotConverter::toItemInfoSnapshot).collect(Collectors.toList()));
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
        return new JobExporter(entityManager).exportFailedItems(jobId, Collections.singletonList(fromPhase), type, encodedAs);
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

        if (!jobState.fatalDiagnosticExists()) {
            jobState.getPhase(State.Phase.PARTITIONING).setBeginDate(new Date());
            try {
                String flowJson = jsonbContext.marshall(addJobParam.getFlow());
                String sinkJson;
                if (addJobParam.getJobInputStream().getJobSpecification().getType() == JobSpecification.Type.ACCTEST) {
                    LOGGER.info("Forcing diff sink on ACCTEST job");
                    sinkJson = jsonbContext.marshall(
                            new Sink(1, 1,
                                    new SinkContent("DiffSink", JndiConstants.JDBC_RESOURCE_SINK_DIFF, "Internal sink used for acceptance test diff functionality")));
                } else {
                    flowJson = new FlowTrimmer(jsonbContext).trim(flowJson);
                    sinkJson = jsonbContext.marshall(addJobParam.getSink());
                }
                jobEntity.setCachedFlow(cacheFlow(flowJson));
                jobEntity.setCachedSink(cacheSink(sinkJson));
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
     *
     * @param jobId                        id of job for which the chunk is to be created
     * @param chunkId                      id of the chunk to be created
     * @param maxChunkSize                 maximum number of items to be associated to the chunk
     * @param dataPartitioner              data partitioner used for item data extraction
     * @param sequenceAnalyserKeyGenerator sequence analyser key generator
     * @param dataFileId                   id of data file from where the items of the chunk originated
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion
     * @throws JobStoreException on referenced entities not found
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ChunkEntity createChunkEntity(
            int jobId,
            int chunkId,
            short maxChunkSize,
            DataPartitioner dataPartitioner,
            SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator,
            String dataFileId)
            throws JobStoreException {

        final Date chunkBegin = new Date();
        ChunkEntity chunkEntity = null;

        // create items
        final ChunkItemEntities chunkItemEntities = createChunkItemEntities(jobId, chunkId, maxChunkSize, dataPartitioner);
        if (chunkItemEntities.size() > 0) {
            // Items were created, so now create the chunk to which they belong
            final StateChange chunkStateChange = chunkItemEntities.chunkStateChange.setBeginDate(chunkBegin);

            SequenceAnalysisData sequenceAnalysisData = initializeSequenceAnalysisData(sequenceAnalyserKeyGenerator, chunkItemEntities);

            final State chunkState = initializeChunkState(chunkItemEntities);

            chunkStateChange.setEndDate(new Date());
            chunkState.updateState(chunkStateChange);

            chunkEntity = initializeChunkEntityAndSetValues(jobId, chunkId, dataFileId, chunkItemEntities, sequenceAnalysisData, chunkState);

            entityManager.persist(chunkEntity);
            entityManager.flush();
            entityManager.refresh(chunkEntity);

            // update job (with exclusive lock)
            final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
            jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
            jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
            updateJobEntityState(jobEntity, chunkStateChange.setBeginDate(null).setEndDate(null));
            entityManager.flush();
        }

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
            throw new JobStoreException(String.format("JobEntity.%s could not be found", jobId));
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
            throw new JobStoreException(String.format("ItemEntity.key{jobId: %s, chunkId: %s, itemId: %s} could not be found", jobId, chunkId, itemId));
        }
        itemEntity.setWorkflowNote(workflowNote);
        return itemEntity;
    }

    /**
     * Adds the sink and flow (cached within a job entity) to a resource bundle.
     * Adds new supplementary process data object containing submitter id and format retrieved from job specification
     *
     * @param jobId of job to bundle resources for
     * @return resource bundle
     * @throws InvalidInputException on failure to retrieve job
     * @throws NullPointerException on null valued input when creating new resource bundle
     */
    @Stopwatch
    public ResourceBundle getResourceBundle(int jobId) throws JobStoreException, NullPointerException {
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
        if (jobEntity == null) {
            throwInvalidInputException(String.format("JobEntity.%d could not be found", jobId), JobError.Code.INVALID_JOB_IDENTIFIER);
        }
        final Flow flow = jobEntity.getCachedFlow().getFlow();
        final Sink sink = jobEntity.getCachedSink().getSink();
        final SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessData(
                jobEntity.getSpecification().getSubmitterId(),
                jobEntity.getSpecification().getFormat());

        return new ResourceBundle(flow, sink, supplementaryProcessData);
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
        final Profiler profiler=new Profiler("pgJobStoreReposiutory.getChunk");
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
                Profiler nestedProfiler=profiler.startNested("loop items");
                final Chunk chunk = new Chunk(jobId, chunkId, type);
                int i=0;
                for (ItemEntity itemEntity : itemEntities) {
                    if (PROCESSED == type) {
                        nestedProfiler.start("Get ProcessintOutcome + nextProcessingOutComme : "+ i);
                        // Special case for chunks containing 'next' items - only relevant in phase PROCESSED
                        chunk.insertItem(itemEntity.getProcessingOutcome(), itemEntity.getNextProcessingOutcome());
                        nestedProfiler.stop();
                    } else {
                        nestedProfiler.start("getChunkItemForPhase " + i);
                        chunk.insertItem(itemEntity.getChunkItemForPhase(phase));
                        nestedProfiler.stop();
                    }
                    ++i;
                }
                chunk.setEncoding(StandardCharsets.UTF_8); // TODO: 15/01/16 This is a temporary solution that should be removed once encoding is removed from Chunk
                return chunk;
            }
            return null;
        } finally {
            LOGGER.info("pgJobStoreReposiutory.getChunk timings:\n"+profiler.toString());
        }
    }

    @Stopwatch
    public ChunkItem getChunkItemForPhase(int jobId, int chunkId, short itemId, State.Phase phase) throws InvalidInputException {
        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, key);
        if (itemEntity == null) {
            throwInvalidInputException(String.format("ItemEntity.Key{jobId:%d, chunkId:%d, itemId:%d} could not be found", jobId, chunkId, itemId), JobError.Code.INVALID_ITEM_IDENTIFIER);
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
            throwInvalidInputException(String.format("ItemEntity.Key{jobId:%d, chunkId:%d, itemId:%d} could not be found", jobId, chunkId, itemId), JobError.Code.INVALID_ITEM_IDENTIFIER);
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
                LOGGER.info("Updating chunk item {} for chunk {} in job {}", chunkItem.getId(), chunk.getChunkId(), chunk.getJobId());
                final ItemEntity.Key itemKey = new ItemEntity.Key((int) chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId());
                final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
                if (itemEntity == null) {
                    throwInvalidInputException(String.format("ItemEntity.%s could not be found", itemKey), JobError.Code.INVALID_ITEM_IDENTIFIER);
                }

                if (itemEntity.getState().phaseIsDone(phase)) {
                    throwDuplicateChunkException(String.format("Aborted attempt to add item %s to already finished %s phase", itemEntity.getKey(), phase), JobError.Code.ILLEGAL_CHUNK);
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
     * Purges all chunks and items associated with specified job,
     * and resets the internal state of the job
     * @param jobId ID of job to be reset
     * @return managed entity for job, or null if no job could be found
     */
    @Stopwatch
    public JobEntity resetJob(int jobId) {
        LOGGER.info("Resetting job {}", jobId);
        final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);

        if (jobEntity != null) {
            final int numberOfPurgedChunks = purgeChunks(jobId);
            LOGGER.info("Purged {} chunks from job {}", numberOfPurgedChunks, jobId);

            final int numberOfPurgedItems = purgeItems(jobId);
            LOGGER.info("Purged {} items from job {}", numberOfPurgedItems, jobId);

            jobEntity.setNumberOfChunks(0);
            jobEntity.setNumberOfItems(0);
            jobEntity.setState(new State());
        }

        return jobEntity;
    }

    /**
     * Deletes all chunks associated with specified job
     * @param jobId ID of job for which to delete chunks
     * @return number of chunks deleted
     */
    @Stopwatch
    public int purgeChunks(int jobId) {
        final Query deleteQuery = entityManager.createQuery("DELETE FROM ChunkEntity e WHERE e.key.jobId = :jobId")
                .setParameter("jobId", jobId);
        return deleteQuery.executeUpdate();
    }

    /**
     * Deletes all items associated with specified job
     * @param jobId ID of job for which to delete items
     * @return number of items deleted
     */
    @Stopwatch
    public int purgeItems(int jobId) {
        final Query deleteQuery = entityManager.createQuery("DELETE FROM ItemEntity e WHERE e.key.jobId = :jobId")
                .setParameter("jobId", jobId);
        return deleteQuery.executeUpdate();
    }

    /**
     * Deletes all entries from the reordered items scratchpad
     * @return number of entries deleted
     */
    @Stopwatch
    public int purgeReorderedItems() {
        final Query deleteQuery = entityManager.createQuery("DELETE FROM ReorderedItemEntity e");
        return deleteQuery.executeUpdate();
    }

    // ***** PRIVATE METHODS *****
    /**
     * private method made with package scope for testing purposes.
     *
     * Adds Flow instance to job-store cache if not already cached
     * @param flowJson Flow document to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued flowJson
     * @throws IllegalArgumentException if given empty-valued flowJson
     * @throws IllegalStateException if unable to create checksum digest
     * entity object to JSON
     */
    @Stopwatch
    FlowCacheEntity cacheFlow(String flowJson) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowJson, "flow");
        final Query storedProcedure = entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE);
        storedProcedure.setParameter("checksum", Md5.asHex(flowJson.getBytes(StandardCharsets.UTF_8)));
        storedProcedure.setParameter("flow", new FlowConverter().convertToDatabaseColumn(flowJson));
        return (FlowCacheEntity) storedProcedure.getSingleResult();
    }

    /**
     * * private method made with package scope for testing purposes.
     *
     * Adds Sink instance to job-store cache if not already cached
     * @param sinkJson Sink document to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued sinkJson
     * @throws IllegalArgumentException if given empty-valued sinkJson
     * @throws IllegalStateException if unable to create checksum digest
     * entity object to JSON
     */
    @Stopwatch
    SinkCacheEntity cacheSink(String sinkJson) throws NullPointerException, IllegalArgumentException, IllegalStateException {
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
    ChunkItemEntities createChunkItemEntities(int jobId, int chunkId, short maxChunkSize, DataPartitioner dataPartitioner) {
        Date nextItemBegin = new Date();
        short itemCounter = 0;
        final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PARTITIONING);
        try {
            /* For each data entry extracted create the containing item entity.
               In case a DataException is thrown by the extraction process a item entity
               is still created but with a serialized JobError as payload instead.
             */
            for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
                if (dataPartitionerResult.isEmpty()) {
                    continue;
                }

                final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
                String trackingId = chunkItem.getTrackingId();
                if(trackingId == null || trackingId.isEmpty()) {
                    // Generate dataio specific tracking id
                    trackingId = TrackingIdGenerator.getTrackingId(jobId, chunkId, itemCounter);
                    chunkItem.setTrackingId(trackingId);
                }
                DBCTrackedLogContext.setTrackingId(trackingId);
                LOGGER.info("Creating chunk item {} for chunk {} in job {}", itemCounter, chunkId, jobId);

                StateChange stateChange = new StateChange()
                        .setPhase(State.Phase.PARTITIONING)
                        .setBeginDate(nextItemBegin)
                        .setEndDate(new Date());

                setItemStateOnChunkItemFromStatus(chunkItemEntities, chunkItem, stateChange);

                final State itemState = new State();
                itemState.updateState(stateChange);

                chunkItem.setId(itemCounter);
                chunkItemEntities.entities.add(persistItemInDatabase(jobId, chunkId, itemCounter++, itemState, chunkItem, dataPartitionerResult.getRecordInfo()));
                if(dataPartitionerResult.getRecordInfo() != null) {
                    chunkItemEntities.keys.addAll(dataPartitionerResult.getRecordInfo().getKeys());
                }

                if (itemCounter == maxChunkSize) {
                    break;
                }
                nextItemBegin = new Date();
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Unrecoverable exception caught during job partitioning of job {}", jobId, e);
            final Diagnostic diagnostic = ObjectFactory.buildFatalDiagnostic(
                    String.format("Unable to complete partitioning at chunk %d item %d: %s",
                            chunkId, itemCounter, e.getMessage()), e);

            final StateChange stateChange = new StateChange()
                    .setPhase(State.Phase.PARTITIONING)
                    .setFailed(1)
                    .setBeginDate(nextItemBegin)
                    .setEndDate(new Date());
            final State itemState = new State();
            itemState.getDiagnostics().add(diagnostic);
            itemState.updateState(stateChange);

            chunkItemEntities.entities.add(persistItemInDatabase(jobId, chunkId, itemCounter, itemState, null, null));
            chunkItemEntities.chunkStateChange.incFailed(1);
        } finally {
            DBCTrackedLogContext.remove();
        }
        return chunkItemEntities;
    }

    /**
     * Creates new item entity with data from the partitioning phase
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk for which items are to be created
     * @param itemId id of item
     * @param state initial state of item
     * @param chunkItem result of the partitioning phase
     * @return created item entity (managed)
     */
    @Stopwatch
    private ItemEntity persistItemInDatabase(int jobId, int chunkId, short itemId, State state, ChunkItem chunkItem, RecordInfo recordInfo) {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(new ItemEntity.Key(jobId, chunkId, itemId));
        itemEntity.setState(state);
        itemEntity.setPartitioningOutcome(chunkItem);
        itemEntity.setRecordInfo(recordInfo);
        entityManager.persist(itemEntity);
        return itemEntity;
    }

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
                throwInvalidInputException(String.format("Trying to add items to %s phase of Chunk[%d,%d]", phase, chunk.getJobId(), chunk.getChunkId()), JobError.Code.ILLEGAL_CHUNK);
        }
    }
    private State initializeChunkState(ChunkItemEntities chunkItemEntities) {
        final State chunkState = new State();
        for (final ItemEntity itemEntity : chunkItemEntities.entities) {
            chunkState.getDiagnostics().addAll(itemEntity.getState().getDiagnostics());
        }
        return chunkState;
    }

    private SequenceAnalysisData initializeSequenceAnalysisData(SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, ChunkItemEntities chunkItemEntities) {
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
            default: throw new IllegalStateException(String.format("Unknown type: '%s'", chunkType));
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

        public ChunkItemEntities() {
            entities = new ArrayList<>();
            chunkStateChange = new StateChange();
            keys = new ArrayList<>();
        }

        public short size() {
            return (short) entities.size();
        }
    }
}