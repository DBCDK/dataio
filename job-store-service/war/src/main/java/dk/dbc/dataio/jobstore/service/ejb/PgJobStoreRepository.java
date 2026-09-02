package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.partioner.DataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.KeyGenerator;
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
import dk.dbc.dataio.jobstore.service.param.AddJobParam;
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
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.log.DBCTrackedLogContext;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static java.lang.String.format;

/**
 * This is an DAO Repository for internal use of the job-store-service hence package scoped methods.
 */
@Stateless
public class PgJobStoreRepository extends RepositoryBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreRepository.class);
    @Inject
    DependencyTrackingService dependencyTrackingService;

    @EJB
    protected JobGateRepository jobGateRepository;

    JSONBContext jsonbContext = new JSONBContext();

    public PgJobStoreRepository withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    public PgJobStoreRepository withJobGateRepository(JobGateRepository jobGateRepository) {
        this.jobGateRepository = jobGateRepository;
        return this;
    }

    /**
     * Creates job listing based on given criteria
     *
     * @param criteria job listing criteria
     * @return list of information snapshots of selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     */
    //@Stopwatch
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return new JobListQuery(entityManager).execute(criteria);
    }

    public long countJobs(JobListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return new JobListQuery(entityManager).execute_count(criteria);
    }

    public List<JobInfoSnapshot> listJobs(String query)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(query, "query");
        return new JobListQuery(entityManager).execute(query);
    }

    public long countJobs(String query) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(query, "query");
        return new JobListQuery(entityManager).count(query);
    }

    public List<Integer> findDependingJobs(int jobId) {
        Query query = entityManager.createNativeQuery("select distinct jobid from dependencytracking where waitingon::jsonb @@ '$[*].jobId==" + jobId + "'");
        query.setParameter(1, jobId);
        @SuppressWarnings("unchecked")
        List<Integer> list = new ArrayList<Integer>(query.getResultList());
        list.remove(Integer.valueOf(jobId));
        return list;
    }

    public int resetStatus(Set<Integer> jobIds, ChunkSchedulingStatus fromStatus, ChunkSchedulingStatus toStatus) {
        return dependencyTrackingService.resetStatus(fromStatus, toStatus, jobIds.toArray(Integer[]::new));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void resetChunk(DependencyTracking e, ChunkSchedulingStatus status) {
        e.setStatus(status);
        entityManager.persist(e);
    }

    public List<ItemInfoSnapshot> listItems(String query)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(query, "query");
        final List<ItemEntity> itemEntities = new ItemListQuery(entityManager).execute(query);
        final List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(itemEntities.size());
        itemInfoSnapshots.addAll(itemEntities.stream()
                .map(ItemEntity::toItemInfoSnapshot)
                .collect(Collectors.toList()));
        return itemInfoSnapshots;
    }

    public long countItems(String query) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(query, "query");
        return new ItemListQuery(entityManager).count(query);
    }

    /**
     * Creates item listing based on given criteria
     *
     * @param criteria item listing criteria
     * @return list of information snapshots of selected items
     * @throws NullPointerException if given null-valued criteria argument
     */
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        final List<ItemEntity> itemEntities = new ItemListQuery(entityManager).execute(criteria);
        final List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(itemEntities.size());
        itemInfoSnapshots.addAll(itemEntities.stream().map(ItemEntity::toItemInfoSnapshot).collect(Collectors.toList()));
        return itemInfoSnapshots;
    }

    public List<Timestamp> listTimeOfChunkCompletion(int jobId) {
        TypedQuery<Timestamp> query = entityManager.createQuery("select c.timeOfCompletion from ChunkEntity c where c.key.jobId = :id", Timestamp.class);
        query.setParameter("id", jobId);
        return query.getResultList();
    }

    /**
     * Exports from a job all chunk items which have failed in a specific phase
     *
     * @param jobId     of the job
     * @param fromPhase specified phase
     * @param type      of export
     * @param encodedAs specified encoding
     * @return byteArrayOutputStream containing the requested items.
     * @throws JobStoreException on general failure to write output stream
     */
    public ByteArrayOutputStream exportFailedItems(int jobId, State.Phase fromPhase, ChunkItem.Type type,
                                                   Charset encodedAs) throws JobStoreException {
        return new JobExporter(entityManager)
                .exportFailedItemsContent(jobId, Collections.singletonList(fromPhase), type, encodedAs)
                .getContent();
    }

    /**
     * Exports all successful chunk items for a given phase for a given job to file in file-store
     *
     * @param jobId                     ID of the job from which to export items
     * @param fromPhase                 phase to export
     * @param fileStoreServiceConnector connector used to upload export
     * @return URL of export
     * @throws JobStoreException on failure to export
     */
    @Stopwatch
    public String exportItemsToFileStore(int jobId, State.Phase fromPhase, FileStoreServiceConnector fileStoreServiceConnector) throws JobStoreException {
        return new JobExporter(entityManager)
                .exportItemsDataToFileStore(jobId, fromPhase, fileStoreServiceConnector);
    }

    /**
     * Tests existance of given job
     *
     * @param jobId ID of job
     * @return true if job exists, otherwise false
     */
    @Stopwatch
    public boolean jobExists(int jobId) {
        return entityManager.find(JobEntity.class, jobId) != null;
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
            jobState.getPhase(State.Phase.PARTITIONING).withBeginDate(new Date());
            try {
                String flowJson = jsonbContext.marshall(addJobParam.getFlow());
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
     * Creates new job entity with all phases set to completed
     *
     * @param addJobParam containing parameter abstraction for the parameters
     *                    needed by PgJobStore.addEmptyJob() method.
     * @return created job entity (managed)
     * @throws JobStoreException if unable to cache associated flow or sink
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public JobEntity createJobEntityForEmptyJob(AddJobParam addJobParam) throws JobStoreException {
        final JobEntity jobEntity = createJobEntity(addJobParam);
        final Date now = new Date();
        updateJobEntityState(jobEntity, new StateChange()
                .setPhase(State.Phase.PARTITIONING).setEndDate(now));
        updateJobEntityState(jobEntity, new StateChange()
                .setPhase(State.Phase.PROCESSING).setEndDate(now));
        updateJobEntityState(jobEntity, new StateChange()
                .setPhase(State.Phase.DELIVERING).setEndDate(now));
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
     * @param submitterId     submitter number
     * @param jobId           id of job for which the chunk is to be created
     * @param chunkId         id of the chunk to be created
     * @param maxChunkSize    maximum number of items to be associated to the chunk
     * @param dataPartitioner data partitioner used for item data extraction
     * @param keyGenerator    dependency tracking key generator
     * @param dataFileId      id of data file from where the items of the chunk originated
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion
     * @throws JobStoreException on referenced entities not found
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Timed
    public ChunkEntity createChunkEntity(long submitterId, int jobId, int chunkId, short maxChunkSize,
                                         DataPartitioner dataPartitioner, KeyGenerator keyGenerator, String dataFileId)
            throws JobStoreException {

        final ChunkEntity chunkEntity = persistChunk(jobId, chunkId, dataFileId);

        // create items
        final ChunkItemEntities chunkItemEntities =
                createChunkItemEntities(submitterId, jobId, chunkId, maxChunkSize, dataPartitioner);
        if (chunkItemEntities.size() > 0) {
            chunkEntity.setNumberOfItems(chunkItemEntities.size());
            chunkEntity.setSequenceAnalysisData(getSequenceAnalysisData(keyGenerator, chunkItemEntities));
            chunkEntity.setContainsLiveHeadOrSectionRecord(containsLiveHeadOrSectionRecord(chunkItemEntities));

            final State chunkState = chunkItemEntities.getChunkState();
            chunkEntity.setState(chunkState);
            if (chunkState.fatalDiagnosticExists()) {
                chunkEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            }

            // update job (with exclusive lock)
            final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
            jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
            jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
            jobEntity.setSkipped(jobEntity.getSkipped() + dataPartitioner.getAndResetSkippedCount());
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
     * <p>
     * This is also where the job's per-job gate comes into existence, see
     * docs/chunk-scheduling-redesign.md, "Barrier Chunks - Per-Job Gate", site B. Three writes
     * belong in this one transaction, under the job row lock taken below and the barrier scope's
     * advisory lock:
     * </p>
     * <ul>
     * <li>{@code data_chunks_expected}, from the {@code dataChunksExpected} parameter. It must
     * <b>not</b> be re-read here: this method increments {@code numberOfChunks} itself, so a read
     * taken here is one too high, the counter can never reach the total and the gate never opens.
     * It must not be written in {@code markJobAsPartitioned} either, because that runs in a
     * separate transaction whose uncommitted write is invisible to the gate verdict below.</li>
     * <li>{@code termination_barrier_lifted = false}, the one place a barrier comes into
     * existence. It stays in the same transaction as the {@code is_termination} row, so a job
     * cannot end up with a termination row and an unset barrier, which is what makes that column's
     * nullable default safe.</li>
     * <li>The termination chunk's own {@code dependencytracking} row, with its gate verdict. The
     * gate is evaluated here and not only on delivery because a job whose data chunks all finish
     * delivering before partitioning ends gets no further delivery to evaluate on. Jobs with zero
     * data chunks are just the extreme case of that.</li>
     * </ul>
     *
     * @param jobId              id of job for which the chunk is to be created
     * @param chunkId            id of the chunk to be created
     * @param dataFileId         for fake chunk
     * @param itemStatus         status for JOB_END item
     * @param dataChunksExpected the job's data-chunk count as read in {@code markJobAsPartitioned}
     *                           before this method runs, which for a job with a termination chunk
     *                           is the same value as {@code chunkId}
     * @param terminationTracker the dependency tracking entry the caller is about to add to the
     *                           map, written to PostgreSQL here so that the row carries a closed
     *                           gate from the moment it exists
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion*
     * @throws JobStoreException on referenced entities not found
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ChunkEntity createJobTerminationChunkEntity(
            int jobId,
            int chunkId,
            String dataFileId, ChunkItem.Status itemStatus,
            int dataChunksExpected,
            DependencyTracking terminationTracker) throws JobStoreException {

        final Date chunkBegin = new Date();

        // Create ChunkItemEntities
        short itemId = 0;

        final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PARTITIONING);
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PROCESSING);

        final ChunkItem chunkItem = new ChunkItem()
                .withId(itemId)
                .withStatus(itemStatus)
                .withType(ChunkItem.Type.JOB_END)
                .withData("Job termination item")
                .withTrackingId(format("%d.JOB_END", jobId));

        final State itemState = new State();
        itemState.updateState(new StateChange().setPhase(State.Phase.PARTITIONING).setBeginDate(chunkBegin).setEndDate(new Date()).setSucceeded(1));
        itemState.updateState(new StateChange().setPhase(State.Phase.PROCESSING).setBeginDate(chunkBegin).setEndDate(new Date()).setSucceeded(1));

        final ItemEntity itemEntity = new ItemEntity()
                .withKey(new ItemEntity.Key(jobId, chunkId, itemId))
                .withState(itemState)
                .withPartitioningOutcome(chunkItem)
                .withProcessingOutcome(chunkItem)
                // Null record id, and thereby a null correlationKey, is deliberate: this
                // item is a per-job barrier, not a bibliographic record. A non-null id
                // would serialise every job's termination item into one broker group and
                // make them share a delivery watermark key. See
                // docs/chunk-scheduling-redesign.md, Open Questions 1.
                .withRecordInfo(new RecordInfo(null));

        entityManager.persist(itemEntity);

        chunkItemEntities.entities.add(itemEntity);

        // ChunkItem Entities created

        // Items were created, so now create the chunk to which they belong
        final StateChange chunkStateChange = chunkItemEntities.chunkStateChange.setBeginDate(chunkBegin);
        SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(new HashSet<>());

        final State chunkState = new State();
        final Date now = new Date();
        chunkState.updateState(new StateChange().setPhase(State.Phase.PARTITIONING).setBeginDate(chunkBegin).setEndDate(now).setSucceeded(1));
        chunkState.updateState(new StateChange().setPhase(State.Phase.PROCESSING).setBeginDate(now).setEndDate(now).setSucceeded(1));

        final ChunkEntity chunkEntity = initializeChunkEntityAndSetValues(jobId, chunkId, dataFileId, chunkItemEntities, sequenceAnalysisData, chunkState);
        entityManager.persist(chunkEntity);
        entityManager.flush();
        entityManager.refresh(chunkEntity);

        // update job (with exclusive lock)
        final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, jobId);
        jobEntity.setNumberOfChunks(jobEntity.getNumberOfChunks() + 1);
        jobEntity.setNumberOfItems(jobEntity.getNumberOfItems() + chunkEntity.getNumberOfItems());
        jobEntity.setDataChunksExpected(dataChunksExpected);
        jobEntity.setTerminationBarrierLifted(false);
        updateJobEntityState(jobEntity, chunkStateChange.setBeginDate(null).setEndDate(null));
        entityManager.flush();

        // Barrier scope locked after the job row, never before, see the lock ordering note on
        // JobGateBean. It serializes this verdict against a concurrent re-trigger for the same
        // (sink, submitter), which would otherwise let both decline and leave the gate closed with
        // nothing left to open it.
        final int sinkId = terminationTracker.getSinkId();
        final int submitter = terminationTracker.getSubmitter();
        jobGateRepository.advisoryLock(sinkId, submitter);

        // The job row lock is held across the verdict, so a concurrent delivery of the job's last
        // data chunk either committed its increment first, in which case the count read here is
        // the higher one, or blocks on the lock above until this transaction commits and then
        // finds the row and evaluates it. There is no interleaving in which both decline.
        //
        // The count is read from the database rather than from jobEntity because the delivery side
        // increments it with a native statement, which leaves the managed entity stale. The total
        // is the parameter rather than a read back of the column just written, so the verdict does
        // not depend on the flush above having happened.
        final boolean gateOpen =
                jobGateRepository.dataChunksDelivered(jobId) >= dataChunksExpected
                        && !jobGateRepository.hasEarlierUndeliveredTermination(sinkId, submitter, jobId);
        jobGateRepository.upsertTerminationRow(terminationTracker.getKey(), sinkId, submitter,
                terminationTracker.getStatus(), terminationTracker.getMatchKeys(), gateOpen);

        return chunkEntity;
    }

    /**
     * @param entityClass class of the Entity
     * @param primaryKey  the primary key
     * @param <T>         the type
     * @return locked entity
     */
    public <T> T getExclusiveAccessFor(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * @param jobEntity   Job Entity
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
     * Advances the sink_record_delivery_watermark row for (sinkId, recordKey) to
     * (jobId, chunkId, itemId), but only if that tuple is newer than what is already
     * stored (see docs/chunk-scheduling-redesign.md, "Upsert on delivery").
     *
     * @param sinkId    sink id
     * @param recordKey opaque, agency-qualified record key
     * @param jobId     job id
     * @param chunkId   chunk id
     * @param itemId    item id
     */
    public void upsertWatermark(long sinkId, String recordKey, int jobId, int chunkId, short itemId) {
        entityManager.createNativeQuery(
                "INSERT INTO sink_record_delivery_watermark " +
                        "       (sink_id, record_key, job_id, chunk_id, item_id, last_modified) " +
                        "VALUES (?1, ?2, ?3, ?4, ?5, now()) " +
                        "ON CONFLICT (sink_id, record_key) DO UPDATE " +
                        "  SET job_id = EXCLUDED.job_id, chunk_id = EXCLUDED.chunk_id, " +
                        "      item_id = EXCLUDED.item_id, last_modified = EXCLUDED.last_modified " +
                        "  WHERE (EXCLUDED.job_id, EXCLUDED.chunk_id, EXCLUDED.item_id) " +
                        "      > (sink_record_delivery_watermark.job_id, " +
                        "         sink_record_delivery_watermark.chunk_id, " +
                        "         sink_record_delivery_watermark.item_id)")
                .setParameter(1, Math.toIntExact(sinkId))
                .setParameter(2, recordKey)
                .setParameter(3, jobId)
                .setParameter(4, chunkId)
                .setParameter(5, itemId)
                .executeUpdate();
    }

    /**
     * sets a workflow note on an existing job. Any workflow previously added will be wiped in the process
     *
     * @param workflowNote the note to set
     * @param jobId        of the job to which a workflow note should be attached.
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
     * @param jobId        of the referenced job
     * @param chunkId      of the referenced chunk
     * @param itemId       of the item to which a workflow note should be attached.
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
     * @throws NullPointerException  on null valued input when creating new resource bundle
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
     * @param type    type of requested chunk
     * @param jobId   id of job containing chunk
     * @param chunkId id of chunk
     * @return chunk representation for given chunk ID, job ID and type or
     * null if no item entities could be found
     * @throws NullPointerException if given null-valued type or if any of
     *                              underlying item entities contains no data for the corresponding phase
     */
    @Stopwatch
    public Chunk getChunk(Chunk.Type type, int jobId, int chunkId) throws NullPointerException {
        final Profiler profiler = new Profiler("pgJobStoreRepository.getChunk");
        try {
            final State.Phase phase = chunkTypeToStatePhase(InvariantUtil.checkNotNullOrThrow(type, "type"));

            profiler.start("execute Query");
            final List<ItemEntity> itemEntities = queryChunkItemEntities(jobId, chunkId);
            profiler.stop();
            if (!itemEntities.isEmpty()) {
                profiler.start("Loop itemEntities");
                final Chunk chunk = new Chunk(jobId, chunkId, type);
                for (ItemEntity itemEntity : itemEntities) {
                    if (PROCESSED == type) {
                        // Special case for chunks containing 'next' items - only relevant in phase PROCESSED
                        chunk.insertItem(itemEntity.getProcessingOutcome(), itemEntity.getNextProcessingOutcome());
                    } else {
                        chunk.insertItem(itemEntity.getChunkItemForPhase(phase));
                    }
                }
                return chunk;
            }
            return null;
        } finally {
            LOGGER.info("pgJobStoreRepository.getChunk timings:\n{}", profiler);
        }
    }

    /**
     * @param jobId   id of job containing chunk
     * @param chunkId id of chunk
     * @return item entities of the given chunk in ascending item ID order,
     * empty if the chunk has no items
     */
    @Stopwatch
    public List<ItemEntity> getChunkItemEntities(int jobId, int chunkId) {
        return queryChunkItemEntities(jobId, chunkId);
    }

    private List<ItemEntity> queryChunkItemEntities(int jobId, int chunkId) {
        final ItemListCriteria criteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId))
                .and(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, chunkId))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));
        return new ItemListQuery(entityManager).execute(criteria);
    }

    @Stopwatch
    public ChunkItem getChunkItemForPhase(int jobId, int chunkId, short itemId, State.Phase phase) throws InvalidInputException {
        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, key);
        if (itemEntity == null) {
            throwInvalidInputException(format("ItemEntity.Key{jobId:%d, chunkId:%d, itemId:%d} could not be found", jobId, chunkId, itemId), JobError.Code.INVALID_ITEM_IDENTIFIER);
        }
        switch (phase) {
            case PARTITIONING:
                return itemEntity.getPartitioningOutcome();
            case PROCESSING:
                return itemEntity.getProcessingOutcome();
            default:
                return itemEntity.getDeliveringOutcome();
        }
    }

    /**
     * Retrieves next processing outcome as chunk item
     *
     * @param jobId   id of job containing chunk
     * @param chunkId id of chunk containing item
     * @param itemId  id of the item
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
     *
     * @param chunk chunk
     * @return item entities compound object
     * @throws DuplicateChunkException if attempting to update already existing chunk
     * @throws InvalidInputException   if unable to find referenced items or if chunk belongs to PARTITIONING
     *                                 phase
     * @throws JobStoreException       Job Store Exception
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
                if(JobsBean.isAborted(chunk.getJobId())) throw new JobAborted(chunk.getJobId());
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                LOGGER.debug("updateChunkItemEntities: updating {} chunk item {}/{}/{}",
                        chunk.getType(), chunk.getJobId(), chunk.getChunkId(), chunkItem.getId());
                final ItemEntity.Key itemKey = new ItemEntity.Key(chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId());
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
     *
     * @param flowJson Flow document to cache
     * @return id of cache line
     * @throws NullPointerException     if given null-valued flowJson
     * @throws IllegalArgumentException if given empty-valued flowJson
     * @throws IllegalStateException    if unable to create checksum digest
     *                                  entity object to JSON
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
     *
     * @param sinkJson Sink document to cache
     * @return id of cache line
     * @throws NullPointerException     if given null-valued sinkJson
     * @throws IllegalArgumentException if given empty-valued sinkJson
     * @throws IllegalStateException    if unable to create checksum digest
     *                                  entity object to JSON
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
     *
     * @param jobId           id of job containing chunk
     * @param chunkId         id of chunk for which items are to be created
     * @param maxChunkSize    maximum number of items to be associated to the chunk
     * @param dataPartitioner data partitioner used for item data extraction
     * @return item entities compound object
     */
    @Stopwatch
    ChunkItemEntities createChunkItemEntities(long submitterId, int jobId, int chunkId, short maxChunkSize,
                                              DataPartitioner dataPartitioner) {
        Date nextItemBegin = new Date();
        short itemCounter = 0;
        final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(State.Phase.PARTITIONING);
        try {
            final SinkContent.SequenceAnalysisOption sequenceAnalysisOption = getSequenceAnalysisOption(jobId);
            for (DataPartitionerResult dataPartitionerResult : dataPartitioner) {
                if(JobsBean.isAborted(jobId)) throw new JobAborted(jobId);
                if (dataPartitionerResult == null || dataPartitionerResult.isEmpty()) {
                    continue;
                }

                final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
                String trackingId = chunkItem.getTrackingId();
                if (trackingId == null || trackingId.trim().isEmpty()) {
                    // Generate dataio specific tracking id
                    RecordInfo recordInfo = dataPartitionerResult.getRecordInfo();
                    if (recordInfo instanceof MarcRecordInfo) {
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
                LOGGER.debug("Creating chunk item {}/{}/{}", jobId, chunkId, itemCounter);

                StateChange stateChange = new StateChange()
                        .setPhase(State.Phase.PARTITIONING)
                        .setBeginDate(nextItemBegin)
                        .setEndDate(new Date());

                setItemStateOnChunkItemFromStatus(chunkItemEntities, chunkItem, stateChange);

                final State itemState = new State();
                itemState.updateState(stateChange);

                chunkItem.withId(itemCounter);

                final ItemEntity itemEntity = new ItemEntity()
                        .withKey(new ItemEntity.Key(jobId, chunkId, itemCounter++))
                        .withState(itemState)
                        .withPartitioningOutcome(chunkItem)
                        .withRecordInfo(dataPartitionerResult.getRecordInfo())
                        .withPositionInDatafile(dataPartitionerResult.getPositionInDatafile());
                entityManager.persist(itemEntity);
                chunkItemEntities.entities.add(itemEntity);

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
        } catch (RuntimeException | CoderMalfunctionError e) {
            /* CoderMalfunctionError has been known to happen due
               to a bug in the dk.dbc.marc.Marc8Charset implementation
               of the MARC-8 character set */

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

            final ItemEntity itemEntity = new ItemEntity()
                    .withKey(new ItemEntity.Key(jobId, chunkId, itemCounter))
                    .withState(itemState);
            entityManager.persist(itemEntity);
            chunkItemEntities.entities.add(itemEntity);
            chunkItemEntities.chunkStateChange.incFailed(1);
        } finally {
            DBCTrackedLogContext.remove();
        }
        return chunkItemEntities;
    }

    /**
     * Updates the job with item information before closing it
     *
     * @param jobId           of the job to preview
     * @param dataPartitioner data partitioner used for item data extraction
     * @return job preview
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public JobEntity preview(int jobId, DataPartitioner dataPartitioner) {
        Date beginDate = new Date();
        int failed = 0;
        int succeeded = 0;
        try {
            for (DataPartitionerResult ignored : dataPartitioner) {
                succeeded++;
            }
        } catch (PrematureEndOfDataException e) {
            throw e;
        } catch (RuntimeException e) {
            failed++;
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

    /**
     * Applies a state change to an item's state. Replaces the entity's State instance
     * rather than mutating the existing one, which is what marks the converted json
     * column dirty (see the note on ItemEntity.state).
     * <p>
     * The counters on the state change are deltas, not absolute totals: State.updateState
     * adds them onto whatever is already persisted. The affected phase closes either when
     * the state change carries an explicit end date, or automatically once its running
     * total reaches the item's PARTITIONING total. Once a phase is closed, further changes
     * to it are silently ignored, so callers whose contribution could arrive twice before
     * the phase closes need their own idempotency check.
     *
     * @param itemEntity  item entity whose state to advance
     * @param stateChange the change to apply
     * @return the item's new state
     * @throws IllegalStateException if the change would close a PROCESSING or DELIVERING
     *                               phase while the item's PARTITIONING phase is still open
     */
    public State updateItemEntityState(ItemEntity itemEntity, StateChange stateChange) {
        final State itemState = new State(itemEntity.getState());
        itemState.updateState(stateChange);
        itemEntity.setState(itemState);
        return itemState;
    }

    private void setOutcomeOnItemEntityFromPhase(Chunk chunk, State.Phase phase, ItemEntity itemEntity, ChunkItem chunkItem) throws InvalidInputException {
        switch (phase) {
            case PROCESSING:
                itemEntity.setProcessingOutcome(chunkItem);
                break;
            case DELIVERING:
                itemEntity.setDeliveringOutcome(chunkItem);
                break;
            case PARTITIONING:
                throwInvalidInputException(format("Trying to add items to %s phase of Chunk[%d,%d]", phase, chunk.getJobId(), chunk.getChunkId()), JobError.Code.ILLEGAL_CHUNK);
        }
    }

    private SequenceAnalysisData getSequenceAnalysisData(KeyGenerator keyGenerator, ChunkItemEntities chunkItemEntities) {
        return new SequenceAnalysisData(keyGenerator.getKeys(chunkItemEntities.keys));
    }

    boolean containsLiveHeadOrSectionRecord(ChunkItemEntities chunkItemEntities) {
        return chunkItemEntities.entities.stream()
                .map(ItemEntity::getRecordInfo)
                .anyMatch(recordInfo -> recordInfo instanceof MarcRecordInfo marcRecordInfo
                        && (marcRecordInfo.isHead() || marcRecordInfo.isSection())
                        && !marcRecordInfo.isDelete());
    }

    private void throwInvalidInputException(String errMsg, JobError.Code jobErrorCode) throws InvalidInputException {
        final JobError jobError = new JobError(jobErrorCode, errMsg, JobError.NO_STACKTRACE);
        throw new InvalidInputException(errMsg, jobError);
    }

    private State.Phase chunkTypeToStatePhase(Chunk.Type chunkType) {
        switch (chunkType) {
            case PARTITIONED:
                return State.Phase.PARTITIONING;
            case PROCESSED:
                return State.Phase.PROCESSING;
            case DELIVERED:
                return State.Phase.DELIVERING;
            default:
                throw new IllegalStateException(format("Unknown type: '%s'", chunkType));
        }
    }

    private void throwDuplicateChunkException(String errMsg, JobError.Code jobErrorCode) throws DuplicateChunkException {
        final JobError jobError = new JobError(jobErrorCode, errMsg, JobError.NO_STACKTRACE);
        throw new DuplicateChunkException(errMsg, jobError);
    }

    // EJB specification dictates Public across EJB's.
    public static class ChunkItemEntities {
        public final List<ItemEntity> entities;
        public final StateChange chunkStateChange;
        public final List<String> keys;

        public ChunkItemEntities() {
            entities = new ArrayList<>();
            chunkStateChange = new StateChange();
            chunkStateChange.setBeginDate(new Date());
            keys = new ArrayList<>();
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
    }

    private SinkContent.SequenceAnalysisOption getSequenceAnalysisOption(int jobId) {
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobId);
        return jobEntity.getCachedSink().getSink().getContent().getSequenceAnalysisOption();
    }
}
