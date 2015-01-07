package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.jobstore.service.digest.Md5;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowConverter;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkConverter;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.DataException;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
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
                                  SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, Flow flow, Sink sink) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            InvariantUtil.checkNotNullOrThrow(dataPartitioner, "dataPartitioner");
            //InvariantUtil.checkNotNullOrThrow(sequenceAnalyserKeyGenerator, "sequenceAnalyserKeyGenerator");
            InvariantUtil.checkNotNullOrThrow(flow, "flow");
            InvariantUtil.checkNotNullOrThrow(sink, "sink");

            LOGGER.info("Adding job");
            final PgJobStore businessObject = sessionContext.getBusinessObject(PgJobStore.class);
            final short maxChunkSize = 10;
            int chunkId = 0;
            ChunkEntity chunkEntity;

            // Creates job entity in its own transactional scope to enable external visibility
            JobEntity jobEntity = businessObject.createJobEntity(jobInputStream, flow, sink);
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
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
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
    public JobEntity createJobEntity(JobInputStream jobInputStream, Flow flow, Sink sink) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final FlowCacheEntity flowCacheEntity = cacheFlow(flow);
            final SinkCacheEntity sinkCacheEntity = cacheSink(sink);
            final JobEntity jobEntity = new JobEntity();
            jobEntity.setEoj(jobInputStream.getIsEndOfJob());
            jobEntity.setPartNumber(jobInputStream.getPartNumber());
            jobEntity.setSpecification(jobInputStream.getJobSpecification());
            jobEntity.setState(new State());
            jobEntity.setCachedFlow(flowCacheEntity);
            jobEntity.setCachedSink(sinkCacheEntity);
            jobEntity.setFlowName(flow.getContent().getName());
            jobEntity.setSinkName(sink.getContent().getName());
            return persistJob(jobEntity);
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

            final ChunkItemEntities chunkItemEntities = createChunkItemEntities(jobId, chunkId, maxChunkSize,
                    dataPartitioner, sequenceAnalyserKeyGenerator);

            if (chunkItemEntities.size() > 0) {
                // Items were created, so now create the chunk to which they belong

                final StateChange chunkStateChange = new StateChange()
                    .setPhase(State.Phase.PARTITIONING)
                    .setBeginDate(chunkBegin);
                if (chunkItemEntities.isFailed()) {
                    chunkStateChange.setSucceeded(chunkItemEntities.size() - 1).setFailed(1);
                } else {
                    chunkStateChange.setSucceeded(chunkItemEntities.size());
                }
                chunkStateChange.setEndDate(new Date());

                final State chunkState = new State();
                chunkState.updateState(chunkStateChange);

                chunkEntity = new ChunkEntity();
                chunkEntity.setKey(new ChunkEntity.Key(chunkId, jobId));
                chunkEntity.setNumberOfItems(chunkItemEntities.size());
                chunkEntity.setDataFileId(dataFileId);
                chunkEntity.setSequenceAnalysisData(new SequenceAnalysisData(Collections.<String>emptySet()));
                chunkEntity.setState(chunkState);
                persistChunk(chunkEntity);

                // update job (with exclusive lock)

                final StateChange jobStateChange = new StateChange()
                    .setPhase(State.Phase.PARTITIONING);
                if (chunkItemEntities.isFailed()) {
                    jobStateChange.setSucceeded(chunkEntity.getNumberOfItems() - 1).setFailed(1);
                } else {
                    jobStateChange.setSucceeded(chunkEntity.getNumberOfItems());
                }

                final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
                jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
                jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
                final State jobState = new State(jobEntity.getState());
                jobState.updateState(jobStateChange);
                jobEntity.setState(jobState);
                entityManager.flush();
            }

            return chunkEntity;
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
     * @param sequenceAnalyserKeyGenerator sequence analyser key generator
     * @return item entities compound object
     * @throws JobStoreException on failure to create item data
     */
    ChunkItemEntities createChunkItemEntities(int jobId, int chunkId, short maxChunkSize, DataPartitionerFactory.DataPartitioner dataPartitioner,
                                              SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            Date nextItemBegin = new Date();
            short itemCounter = 0;
            final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
            try {
                /* For each data entry extracted create the containing item entity.
                   In case a DataException is thrown by the extraction process a item entity
                   is still created but with a serialized version of the DataException as
                   payload instead.
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

                    chunkItemEntities.getEntities().add(createItem(jobId, chunkId, itemCounter++, itemState, itemData));

                    if (itemCounter == maxChunkSize) {
                        break;
                    }
                    nextItemBegin = new Date();
                }
            } catch (DataException e) {
                LOGGER.warn("Exception caught during job partitioning", e);
                final ItemData itemData;
                try {
                    itemData = new ItemData(Base64Util.base64encode(jsonbBean.getContext().marshall(e)), StandardCharsets.UTF_8);
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

                chunkItemEntities.getEntities().add(createItem(jobId, chunkId, itemCounter, itemState, itemData));
                chunkItemEntities.setFailed(true);
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
            return persistItem(itemEntity);
        } finally {
            LOGGER.trace("Operation took {} milliseconds", stopWatch.getElapsedTime());
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

    /**
     * Persist an item in the job-store
     * <p>
     * Note that the timeOfCreation and timeOfLastModification fields will be set
     * automatically by the underlying database.
     * </p>
     * @param item ItemEntity instance to be persisted
     * @return managed JobEntity instance
     * @throws NullPointerException if given null-valued item
     */
    ItemEntity persistItem(ItemEntity item) {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(item, "item");
            entityManager.persist(item);
            //entityManager.flush();
            //entityManager.refresh(item);
            return item;
        } finally {
            LOGGER.trace("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Persists a chunk in the job-store
     * <p>
     * Note that the timeOfCreation and timeOfLastModification fields will be set
     * automatically by the underlying database.
     * </p>
     * @param chunk ChunkEntity instance to be persisted
     * @return managed ChunkEntity instance
     * @throws NullPointerException if given null-valued chunk
     */
    ChunkEntity persistChunk(ChunkEntity chunk) {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
            entityManager.persist(chunk);
            entityManager.flush();
            entityManager.refresh(chunk);
            return chunk;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Persists a job in the job-store
     * <p>
     * Note that the id, timeOfCreation and timeOfLastModification fields will be set
     * automatically by the underlying database.
     * </p>
     * @param job JobEntity instance to be persisted
     * @return managed JobEntity instance
     * @throws NullPointerException if given null-valued job
     */
    JobEntity persistJob(JobEntity job) {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(job, "job");
            entityManager.persist(job);
            entityManager.flush();
            entityManager.refresh(job);
            return job;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private <T> T getExclusiveAccessFor(Class<T> entityClass, int id) {
        return entityManager.find(entityClass, id, LockModeType.PESSIMISTIC_WRITE);
    }

    /* Chunk item entities compound class
     */
    private static class ChunkItemEntities {
        private final List<ItemEntity> entities;
        private boolean isFailed = false;

        public ChunkItemEntities() {
            entities = new ArrayList<>();
        }

        public List<ItemEntity> getEntities() {
            return entities;
        }

        public boolean isFailed() {
            return isFailed;
        }

        public void setFailed(boolean isFailed) {
            this.isFailed = isFailed;
        }

        public int size() {
            return entities.size();
        }
    }
}
