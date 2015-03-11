package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.service.digest.Md5;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowConverter;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemListQuery;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobListQuery;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkConverter;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.DataException;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the job-store database through persistence layer
 */
@Stateless
public class PgJobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStore.class);

    @Resource
    SessionContext sessionContext;

    @EJB
    JSONBBean jsonbBean;

    @PersistenceContext(unitName = "jobstorePU")
    EntityManager entityManager;

    /**
     * Adds new job job, chunk and item entities in the underlying data store from given job input stream
     * @param jobInputStream job input stream
     * @param dataPartitioner data partitioner used for data extraction and partitioning
     * @param sequenceAnalyserKeyGenerator sequence analyser key generator
     * @param flow specific version of flow to be cached for job
     * @param sink specific version of sink to be cached for job
     * @return information snapshot of added job
     * @throws NullPointerException if given any null-valued argument
     * @throws JobStoreException on failure to add job
     */
    public JobInfoSnapshot addJob(JobInputStream jobInputStream, DataPartitionerFactory.DataPartitioner dataPartitioner,
                                  SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, Flow flow, Sink sink, FlowStoreReferences flowStoreReferences) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            InvariantUtil.checkNotNullOrThrow(dataPartitioner, "dataPartitioner");
            InvariantUtil.checkNotNullOrThrow(sequenceAnalyserKeyGenerator, "sequenceAnalyserKeyGenerator");
            InvariantUtil.checkNotNullOrThrow(flow, "flow");
            InvariantUtil.checkNotNullOrThrow(sink, "sink");
            InvariantUtil.checkNotNullOrThrow(flowStoreReferences, "flowStoreReferences");

            LOGGER.info("Adding job");
            final PgJobStore businessObject = sessionContext.getBusinessObject(PgJobStore.class);

            // Creates job entity in its own transactional scope to enable external visibility
            JobEntity jobEntity = businessObject.createJobEntity(jobInputStream, flow, sink, flowStoreReferences);

            final short maxChunkSize = 10;
            int chunkId = 0;
            ChunkEntity chunkEntity;
            do {
                // Creates each chunk entity (and associated item entities) in its own
                // transactional scope to enable external visibility of job creation progress
                chunkEntity = businessObject.createChunkEntity(jobEntity.getId(), chunkId++, maxChunkSize,
                        dataPartitioner, sequenceAnalyserKeyGenerator, jobInputStream.getJobSpecification().getDataFile());
            } while (chunkEntity != null);

            // Job partitioning is now done - signalled by setting the endDate property of the PARTITIONING phase.

            final StateChange jobStateChange = new StateChange()
                    .setPhase(State.Phase.PARTITIONING)
                    .setEndDate(new Date());

            jobEntity = getExclusiveAccessFor(JobEntity.class, jobEntity.getId());
            final State jobState = new State(jobEntity.getState());
            jobState.updateState(jobStateChange);
            jobEntity.setState(jobState);
            entityManager.flush();

            return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds chunk by updating existing items, chunk and job entities in the underlying data store.
     * @param chunk external chunk
     * @return information snapshot of updated job
     * @throws NullPointerException if given null-valued chunk argument
     * @throws InvalidInputException if unable to find referenced items, if external chunk belongs to PARTITIONING phase
     * or if external chunk contains a number of items not matching that of the internal chunk entity
     * @throws JobStoreException if unable to find referenced chunk or job entities
     */
    public JobInfoSnapshot addChunk(ExternalChunk chunk) throws NullPointerException, JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
            LOGGER.info("Adding chunk[{},{}]", chunk.getJobId(), chunk.getChunkId());

            final ChunkEntity.Key chunkKey =  new ChunkEntity.Key((int) chunk.getChunkId(), (int) chunk.getJobId());
            final ChunkEntity chunkEntity = getExclusiveAccessFor(ChunkEntity.class, chunkKey);
            if (chunkEntity == null) {
                throw new JobStoreException(String.format("ChunkEntity.%s could not be found", chunkKey));
            }

            // update items

            final ChunkItemEntities chunkItemEntities = updateChunkItemEntities(chunk);

            if (chunkItemEntities.size() == chunkEntity.getNumberOfItems()) {
                // update chunk

                final State.Phase phase = chunkItemEntities.chunkStateChange.getPhase();
                final Date chunkPhaseBeginDate = chunkItemEntities.entities.get(0).getState().getPhase(phase).getBeginDate();
                final Date chunkPhaseEndDate = chunkItemEntities.entities.get(chunkItemEntities.size() - 1).getState().getPhase(phase).getEndDate();
                final StateChange chunkStateChange = chunkItemEntities.chunkStateChange
                    .setBeginDate(chunkPhaseBeginDate)
                    .setEndDate(chunkPhaseEndDate);

                final State chunkState = new State(chunkEntity.getState());
                chunkState.updateState(chunkStateChange);
                chunkEntity.setState(chunkState);
                if(chunkState.allPhasesAreDone()) {
                    chunkEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                }

                // update job

                final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, chunkEntity.getKey().getJobId());
                if (jobEntity == null) {
                    throw new JobStoreException(String.format("JobEntity.%d could not be found",
                            chunkEntity.getKey().getJobId()));
                }

                final State jobState = new State(jobEntity.getState());
                jobState.updateState(chunkStateChange
                        .setBeginDate(null)
                        .setEndDate(null));
                jobEntity.setState(jobState);
                if(jobState.allPhasesAreDone()) {
                    jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                }
                entityManager.flush();
                entityManager.refresh(jobEntity);

                return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
            } else {
                final String errMsg = String.format("Chunk[%d,%d] contains illegal number of items %d when %d expected",
                        chunk.getJobId(), chunk.getChunkId(), chunk.size(), chunkEntity.getNumberOfItems());
                final JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, errMsg, null);
                throw new InvalidInputException(errMsg, jobError);
            }
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    /**
     * Creates job listing based on given criteria
     * @param criteria job listing criteria
     * @return list of information snapshots of selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     */
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            return new JobListQuery(entityManager).execute(criteria);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Creates item listing based on given criteria
     * @param criteria item listing criteria
     * @return list of information snapshots of selected items
     * @throws NullPointerException if given null-valued criteria argument
     */
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) throws NullPointerException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            return new ItemListQuery(entityManager).execute(criteria);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Creates new job entity and caches associated Flow and Sink as needed.
     * <p>
     * CAVEAT: Even though this method is publicly available it is <b>NOT</b>
     * intended for use outside of this class - accessibility is only so defined
     * to allow the method to be called internally as an EJB business method.
     * </p>
     * @param jobInputStream job input stream
     * @param flow flow associated with job
     * @param sink sink associated with job
     * @return created job entity (managed)
     * @throws JobStoreException if unable to cache associated flow or sink
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public JobEntity createJobEntity(JobInputStream jobInputStream, Flow flow, Sink sink, FlowStoreReferences flowStoreReferences) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final State jobState = new State();
            jobState.getPhase(State.Phase.PARTITIONING).setBeginDate(new Date());
            final FlowCacheEntity flowCacheEntity = cacheFlow(flow);
            final SinkCacheEntity sinkCacheEntity = cacheSink(sink);
            final JobEntity jobEntity = new JobEntity();
            jobEntity.setEoj(jobInputStream.getIsEndOfJob());
            jobEntity.setPartNumber(jobInputStream.getPartNumber());
            jobEntity.setSpecification(jobInputStream.getJobSpecification());
            jobEntity.setState(jobState);
            jobEntity.setCachedFlow(flowCacheEntity);
            jobEntity.setCachedSink(sinkCacheEntity);
            jobEntity.setFlowStoreReferences(flowStoreReferences);
            entityManager.persist(jobEntity);
            entityManager.flush();
            entityManager.refresh(jobEntity);
            return jobEntity;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Creates new chunk and associated data item entities and updates the state of the containing job
     * <p>
     * CAVEAT: Even though this method is publicly available it is <b>NOT</b>
     * intended for use outside of this class - accessibility is only so defined
     * to allow the method to be called internally as an EJB business method.
     * </p>
     * @param jobId id of job for which the chunk is to be created
     * @param chunkId id of the chunk to be created
     * @param maxChunkSize maximum number of items to be associated to the chunk
     * @param dataPartitioner data partitioner used for item data extraction
     * @param sequenceAnalyserKeyGenerator sequence analyser key generator
     * @param dataFileId id of data file from where the items of the chunk originated
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion
     * @throws JobStoreException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ChunkEntity createChunkEntity(int jobId, int chunkId, short maxChunkSize, DataPartitionerFactory.DataPartitioner dataPartitioner,
                                         SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, String dataFileId) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final Date chunkBegin = new Date();
            ChunkEntity chunkEntity = null;

            // create items

            final ChunkItemEntities chunkItemEntities = createChunkItemEntities(jobId, chunkId, maxChunkSize, dataPartitioner);

            if (chunkItemEntities.size() > 0) {
                // Items were created, so now create the chunk to which they belong

                final StateChange chunkStateChange = chunkItemEntities.chunkStateChange
                    .setBeginDate(chunkBegin);

                SequenceAnalysisData sequenceAnalysisData;
                if (chunkStateChange.getFailed() > 0) {
                    sequenceAnalysisData = new SequenceAnalysisData(Collections.<String>emptySet());
                } else {
                    sequenceAnalysisData = new SequenceAnalysisData(
                        sequenceAnalyserKeyGenerator.generateKeys(chunkItemEntities.records));
                }
                chunkStateChange.setEndDate(new Date());

                final State chunkState = new State();
                chunkState.updateState(chunkStateChange);

                chunkEntity = new ChunkEntity();
                chunkEntity.setKey(new ChunkEntity.Key(chunkId, jobId));
                chunkEntity.setNumberOfItems(chunkItemEntities.size());
                chunkEntity.setDataFileId(dataFileId);
                chunkEntity.setSequenceAnalysisData(sequenceAnalysisData);
                chunkEntity.setState(chunkState);
                entityManager.persist(chunkEntity);
                entityManager.flush();
                entityManager.refresh(chunkEntity);

                // update job (with exclusive lock)

                final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
                jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
                jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
                final State jobState = new State(jobEntity.getState());
                jobState.updateState(chunkStateChange
                    .setBeginDate(null)
                    .setEndDate(null)
                );
                jobEntity.setState(jobState);
                entityManager.flush();
            }

            return chunkEntity;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
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
    public ResourceBundle getResourceBundle(int jobId) throws JobStoreException, NullPointerException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
            if (jobEntity == null) {
                final String errMsg = String.format("JobEntity.%d could not be found", jobId);
                final JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, errMsg, null);
                throw new InvalidInputException(errMsg, jobError);
            }
            final Flow flow = jobEntity.getCachedFlow().getFlow();
            final Sink sink = jobEntity.getCachedSink().getSink();
            final SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessData(
                    jobEntity.getSpecification().getSubmitterId(),
                    jobEntity.getSpecification().getFormat());

            return new ResourceBundle(flow, sink, supplementaryProcessData);
        } finally {
        LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Creates item entities for given chunk using data extracted via given data partitioner
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk for which items are to be created
     * @param maxChunkSize maximum number of items to be associated to the chunk
     * @param dataPartitioner data partitioner used for item data extraction
     * @return item entities compound object
     * @throws JobStoreException on failure to create item data
     */
    ChunkItemEntities createChunkItemEntities(int jobId, int chunkId, short maxChunkSize,
                                              DataPartitionerFactory.DataPartitioner dataPartitioner) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            Date nextItemBegin = new Date();
            short itemCounter = 0;
            final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
            chunkItemEntities.chunkStateChange.setPhase(State.Phase.PARTITIONING);
            try {
                /* For each data entry extracted create the containing item entity.
                   In case a DataException is thrown by the extraction process a item entity
                   is still created but with a serialized JobError as payload instead.
                 */

                for (String record : dataPartitioner) {
                    final ItemData itemData = new ItemData(Base64Util.base64encode(record), dataPartitioner.getEncoding());

                    final StateChange stateChange = new StateChange()
                        .setPhase(State.Phase.PARTITIONING)
                        .setSucceeded(1)
                        .setBeginDate(nextItemBegin)
                        .setEndDate(new Date());
                    final State itemState = new State();
                    itemState.updateState(stateChange);

                    chunkItemEntities.entities.add(createItem(jobId, chunkId, itemCounter++, itemState, itemData));
                    chunkItemEntities.records.add(record);
                    chunkItemEntities.chunkStateChange.incSucceeded(1);

                    if (itemCounter == maxChunkSize) {
                        break;
                    }
                    nextItemBegin = new Date();
                }

            } catch (DataException e) {
                LOGGER.warn("Exception caught during job partitioning", e);
                final ItemData itemData;
                try {
                    final JobError jobError = new JobError(
                            JobError.Code.INVALID_DATA, e.getMessage(), ServiceUtil.stackTraceToString(e));
                    itemData = new ItemData(
                            Base64Util.base64encode(jsonbBean.getContext().marshall(jobError)), StandardCharsets.UTF_8);
                } catch (JSONBException ex) {
                    throw new JobStoreException("Exception caught during error handling", ex);
                }

                final StateChange stateChange = new StateChange()
                    .setPhase(State.Phase.PARTITIONING)
                    .setFailed(1)
                    .setBeginDate(nextItemBegin)
                    .setEndDate(new Date());
                final State itemState = new State();
                itemState.updateState(stateChange);

                chunkItemEntities.entities.add(createItem(jobId, chunkId, itemCounter, itemState, itemData));
                chunkItemEntities.chunkStateChange.incFailed(1);
            }
            return chunkItemEntities;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates item entities for given chunk
     * @param chunk external chunk
     * @return item entities compound object
     * @throws InvalidInputException if unable to find referenced items or if external chunk belongs to PARTITIONING
     * phase
     * @throws JobStoreException
     */
    ChunkItemEntities updateChunkItemEntities(ExternalChunk chunk) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            Date nextItemBegin = new Date();

            final State.Phase phase = chunkTypeToStatePhase(chunk.getType());
            final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
            chunkItemEntities.chunkStateChange.setPhase(phase);

            for (ChunkItem ci : chunk) {
                final ItemEntity.Key itemKey = new ItemEntity.Key((int) chunk.getJobId(), (int) chunk.getChunkId(), (short) ci.getId());
                final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
                if (itemEntity == null) {
                    final String errMsg = String.format("ItemEntity.%s could not be found", itemKey);
                    final JobError jobError = new JobError(JobError.Code.INVALID_ITEM_IDENTIFIER, errMsg, null);
                    throw new InvalidInputException(errMsg, jobError);
                }

                chunkItemEntities.entities.add(itemEntity);

                if (itemEntity.getState().phaseIsDone(phase)) {
                    LOGGER.warn("Aborted attempt to add item {} to already finished {} phase", itemEntity.getKey(), phase);
                    break;
                }

                final ItemData itemData = new ItemData(ci.getData(), StandardCharsets.UTF_8);   // ToDo: ExternalChunk type must contain encoding

                final StateChange itemStateChange = new StateChange()
                        .setPhase(phase)
                        .setBeginDate(nextItemBegin)                                            // ToDo: ExternalChunk type must contain beginDate
                        .setEndDate(new Date());                                                // ToDo: ExternalChunk type must contain endDate

                switch (phase) {
                    case PROCESSING: itemEntity.setProcessingOutcome(itemData);
                        break;
                    case DELIVERING: itemEntity.setDeliveringOutcome(itemData);
                        break;
                    case PARTITIONING:
                        final String errMsg = String.format("Trying to add items to %s phase of Chunk[%d,%d]",
                                phase, chunk.getJobId(), chunk.getChunkId());
                        final JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, errMsg, null);
                        throw new InvalidInputException(errMsg, jobError);
                }

                switch (ci.getStatus()) {
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

                final State itemState = new State(itemEntity.getState());
                itemState.updateState(itemStateChange);
                itemEntity.setState(itemState);
                if(itemEntity.getState().allPhasesAreDone()) {
                    itemEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                }
                nextItemBegin = new Date();
            }
            return chunkItemEntities;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Creates new item entity with data from the partitioning phase
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk for which items are to be created
     * @param itemId id of item
     * @param state initial state of item
     * @param data result of the partitioning phase
     * @return created item entity (managed)
     */
    ItemEntity createItem(int jobId, int chunkId, short itemId, State state, ItemData data) {
        final StopWatch stopWatch = new StopWatch();
        try {
            final ItemEntity itemEntity = new ItemEntity();
            itemEntity.setKey(new ItemEntity.Key(jobId, chunkId, itemId));
            itemEntity.setState(state);
            itemEntity.setPartitioningOutcome(data);
            entityManager.persist(itemEntity);
            return itemEntity;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds Sink instance to job-store cache if not already cached
     * @param sink Sink object to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued sink
     * @throws IllegalStateException if unable to create checksum digest
     * @throws JobStoreException on failure to marshall
     * entity object to JSON
     */
    SinkCacheEntity cacheSink(Sink sink) throws NullPointerException, IllegalStateException, JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(sink, "sink");
            final Query storedProcedure = entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE);
            storedProcedure.setParameter("checksum", Md5.asHex(jsonbBean.getContext().marshall(sink).getBytes(StandardCharsets.UTF_8)));
            storedProcedure.setParameter("sink", new SinkConverter().convertToDatabaseColumn(sink));
            return (SinkCacheEntity) storedProcedure.getSingleResult();
        } catch (JSONBException e) {
            throw new JobStoreException("Exception caught during job-store operation", e);
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds Flow instance to job-store cache if not already cached
     * @param flow Flow object to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued flow
     * @throws IllegalStateException if unable to create checksum digest
     * @throws JobStoreException on failure to marshall
     * entity object to JSON
     */
    FlowCacheEntity cacheFlow(Flow flow) throws NullPointerException, IllegalStateException, JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(flow, "flow");
            final Query storedProcedure = entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE);
            storedProcedure.setParameter("checksum", Md5.asHex(jsonbBean.getContext().marshall(flow).getBytes(StandardCharsets.UTF_8)));
            storedProcedure.setParameter("flow", new FlowConverter().convertToDatabaseColumn(flow));
            return (FlowCacheEntity) storedProcedure.getSingleResult();
        } catch (JSONBException e) {
            throw new JobStoreException("Exception caught during job-store operation", e);
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private <T> T getExclusiveAccessFor(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE);
    }

    private State.Phase chunkTypeToStatePhase(ExternalChunk.Type chunkType) {
        switch (chunkType) {
            case PARTITIONED: return State.Phase.PARTITIONING;
            case PROCESSED:   return State.Phase.PROCESSING;
            case DELIVERED:   return State.Phase.DELIVERING;
            default:          return null;
        }
    }

    /* Chunk item entities compound class
     */
    static class ChunkItemEntities {
        public final List<ItemEntity> entities;
        public final List<String> records;
        public final StateChange chunkStateChange;

        public ChunkItemEntities() {
            entities = new ArrayList<>();
            records = new ArrayList<>();
            chunkStateChange = new StateChange();
        }

        public short size() {
            return (short) entities.size();
        }
    }
}
