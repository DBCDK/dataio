package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
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
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.service.util.FlowTrimmer;
import dk.dbc.dataio.jobstore.service.util.ItemInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
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
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static dk.dbc.dataio.commons.types.ExternalChunk.Type.PROCESSED;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the job-store database through persistence layer
 */
@Stateless
public class PgJobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStore.class);

    @Resource
    SessionContext sessionContext;

    /* These instances are not private otherwise they were not accessible from automatic test */
    @EJB
    JobSchedulerBean jobSchedulerBean;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @PersistenceContext(unitName = "jobstorePU")
    EntityManager entityManager;

    JSONBContext jsonbContext = new JSONBContext();

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
        AddJobParam param = new AddJobParam(jobInputStream, flowStoreServiceConnectorBean.getConnector(), fileStoreServiceConnectorBean.getConnector());
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
        LOGGER.info("Adding job");
        final PgJobStore businessServiceProxy = getProxyToSelf();

        // Creates job entity in its own transactional scope to enable external visibility
        JobEntity jobEntity = businessServiceProxy.createJobEntity(addJobParam);

        businessServiceProxy.handlePartitioningAsynchronously(addJobParam, businessServiceProxy, jobEntity);

        return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
    }

    /**
     * OBS: this method is a public wrapper to support asynchronous behavior
     *
     * @param addJobParam
     * @param businessServiceProxy
     * @param jobEntity
     * @throws JobStoreException
     */
    @Asynchronous
    public void handlePartitioningAsynchronously(AddJobParam addJobParam, PgJobStore businessServiceProxy, JobEntity jobEntity) throws JobStoreException {
        handlePartitioning(addJobParam, businessServiceProxy, jobEntity);
    }

    /**
     * This method has package scope so testing is possible
     * @param addJobParam
     * @param businessServiceProxy
     * @param jobEntity
     * @return
     * @throws JobStoreException
     */
     JobInfoSnapshot handlePartitioning(AddJobParam addJobParam, PgJobStore businessServiceProxy, JobEntity jobEntity) throws JobStoreException {
        final List<Diagnostic> abortDiagnostics = new ArrayList<>(0);

        // Continue only if timeOfCompletion is not set (all required remote entities could be located).
        if (jobEntity.getTimeOfCompletion() == null) {

            doPartitioningToChunksAndItems(addJobParam, businessServiceProxy, jobEntity, abortDiagnostics);

            // Job partitioning is now done - signalled by setting the endDate property of the PARTITIONING phase.
            final StateChange jobStateChange = new StateChange().setPhase(State.Phase.PARTITIONING).setEndDate(new Date());

            jobEntity = getExclusiveAccessFor(JobEntity.class, jobEntity.getId());
            updateJobEntityState(jobEntity, jobStateChange);
            if (!abortDiagnostics.isEmpty()) {
                abortJob(jobEntity, abortDiagnostics);
            }
        }
        entityManager.flush();
        logTimerMessage(jobEntity);

        // Important that this is done after partitioning is done otherwise DataPartitioner is not updated with file info - hence filesize is 0.
        if (!jobEntity.getState().fatalDiagnosticExists()) {
            try {
                compareByteSize(addJobParam.getDataFileId(), addJobParam.getDataPartitioner());
            } catch (IOException e) {
                throw new JobStoreException("Error reading data file", e);
            }
        }

        addJobParam.closeDataFile();

        return JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);
    }
    private void doPartitioningToChunksAndItems(
            AddJobParam addJobParam,
            PgJobStore businessServiceProxy,
            JobEntity jobEntity,
            List<Diagnostic> abortDiagnostics) throws JobStoreException {

        final short maxChunkSize = 10;
        int chunkId = 0;
        ChunkEntity chunkEntity;
        do {
            // Creates each chunk entity (and associated item entities) in its own
            // transactional scope to enable external visibility of job creation progress
            chunkEntity = businessServiceProxy.createChunkEntity(
                    jobEntity.getId(),
                    chunkId++,
                    maxChunkSize,
                    addJobParam.getDataPartitioner(),
                    addJobParam.getSequenceAnalyserKeyGenerator(),
                    addJobParam.getJobInputStream().getJobSpecification().getDataFile());

            if (chunkEntity == null) {
                break;
            }
            if (chunkEntity.getState().fatalDiagnosticExists()) {
                // Partitioning resulted in unrecoverable error - set diagnostic to force job abortion
                abortDiagnostics.addAll(chunkEntity.getState().getDiagnostics());
                break;
            }
            jobSchedulerBean.scheduleChunk(chunkEntity.toCollisionDetectionElement(), addJobParam.getSink());
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

            final State chunkState = updateChunkEntityState(chunkEntity, chunkStateChange);
            if(chunkState.allPhasesAreDone()) {
                chunkEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                jobSchedulerBean.releaseChunk((ChunkIdentifier) chunkEntity.toCollisionDetectionElement().getIdentifier());
            }

            // update job
            final JobEntity jobEntity = getExclusiveAccessFor(JobEntity.class, chunkEntity.getKey().getJobId());
            if (jobEntity == null) {
                throw new JobStoreException(String.format("JobEntity.%d could not be found",
                        chunkEntity.getKey().getJobId()));
            }
            final State jobState = updateJobEntityState(jobEntity, chunkStateChange.setBeginDate(null).setEndDate(null));
            if (jobState.allPhasesAreDone()) {
                jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
                logTimerMessage(jobEntity);
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
    }


    /**
     * Creates job listing based on given criteria
     * @param criteria job listing criteria
     * @return list of information snapshots of selected jobs
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return new JobListQuery(entityManager).execute(criteria);
    }

    /**
     * Creates chunk collision detection element listing based on given criteria
     * @param criteria chunk listing criteria
     * @return list of collision detection elements
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public List<CollisionDetectionElement> listChunksCollisionDetectionElements(ChunkListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        final List<ChunkEntity> chunkEntities = new ChunkListQuery(entityManager).execute(criteria);
        final List<CollisionDetectionElement> collisionDetectionElements = new ArrayList<>(chunkEntities.size());
        for(ChunkEntity chunkEntity : chunkEntities) {
            collisionDetectionElements.add(chunkEntity.toCollisionDetectionElement());
        }
        return collisionDetectionElements;
    }

    /**
     * Creates item listing based on given criteria
     * @param criteria item listing criteria
     * @return list of information snapshots of selected items
     * @throws NullPointerException if given null-valued criteria argument
     */
    @Stopwatch
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        final List<ItemEntity> itemEntities = new ItemListQuery(entityManager).execute(criteria);
        final List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(itemEntities.size());
        for (ItemEntity itemEntity : itemEntities) {
            itemInfoSnapshots.add(ItemInfoSnapshotConverter.toItemInfoSnapshot(itemEntity));
        }
        return itemInfoSnapshots;
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
                    sinkJson = jsonbContext.marshall(new Sink(1, 1, new SinkContent(
                            "DiffSink", JndiConstants.JDBC_RESOURCE_SINK_DIFF, "Internal sink used for acceptance test diff functionality")));
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
     * @param jobId id of job for which the chunk is to be created
     * @param chunkId id of the chunk to be created
     * @param maxChunkSize maximum number of items to be associated to the chunk
     * @param dataPartitioner data partitioner used for item data extraction
     * @param sequenceAnalyserKeyGenerator sequence analyser key generator
     * @param dataFileId id of data file from where the items of the chunk originated
     * @return created chunk entity (managed) or null of no chunk was created as a result of data exhaustion
     * @throws JobStoreException on referenced entities not found
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ChunkEntity createChunkEntity(
            int jobId,
            int chunkId,
            short maxChunkSize,
            DataPartitionerFactory.DataPartitioner dataPartitioner,
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

                SequenceAnalysisData sequenceAnalysisData = initializeSequenceAnalysisData(sequenceAnalyserKeyGenerator, chunkItemEntities, chunkStateChange);

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
    private SequenceAnalysisData initializeSequenceAnalysisData(SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator, ChunkItemEntities chunkItemEntities, StateChange chunkStateChange) {
        SequenceAnalysisData sequenceAnalysisData;
        if (chunkStateChange.getFailed() > 0) {
            sequenceAnalysisData = new SequenceAnalysisData(Collections.<String>emptySet());
        } else {
            sequenceAnalysisData = new SequenceAnalysisData(sequenceAnalyserKeyGenerator.generateKeys(chunkItemEntities.records));
        }
        return sequenceAnalysisData;
    }
    private State initializeChunkState(ChunkItemEntities chunkItemEntities) {
        final State chunkState = new State();
        for (final ItemEntity itemEntity : chunkItemEntities.entities) {
            chunkState.getDiagnostics().addAll(itemEntity.getState().getDiagnostics());
        }
        return chunkState;
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
    public ExternalChunk getChunk(ExternalChunk.Type type, int jobId, int chunkId) throws NullPointerException {
        final State.Phase phase = chunkTypeToStatePhase(InvariantUtil.checkNotNullOrThrow(type, "type"));
        final ItemListCriteria criteria = new ItemListCriteria()
                .where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId))
                    .and(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, chunkId))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.ASC));

        final List<ItemEntity> itemEntities = new ItemListQuery(entityManager).execute(criteria);
        if (itemEntities.size() > 0) {
            final ExternalChunk chunk = new ExternalChunk(jobId, chunkId, type);
            for (ItemEntity itemEntity : itemEntities) {
                if (PROCESSED == type) {
                    // Special case for chunks containing 'next' items - only relevant in phase PROCESSED
                    chunk.insertItem(itemEntity.toChunkItem(phase), itemEntity.getNextProcessingOutcome());
                } else {
                    chunk.insertItem(itemEntity.toChunkItem(phase));
                }
            }
            chunk.setEncoding(itemEntities.get(0).getEncodingForPhase(phase));
            return chunk;
        }
        return null;
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
    @Stopwatch
    ChunkItemEntities createChunkItemEntities(
            int jobId,
            int chunkId,
            short maxChunkSize,
            DataPartitionerFactory.DataPartitioner dataPartitioner)
                throws JobStoreException {

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
                final ItemData itemData = new ItemData(StringUtil.base64encode(record), dataPartitioner.getEncoding());

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

        } catch (UnrecoverableDataException e) {
            LOGGER.warn("Unrecoverable exception caught during job partitioning of job {}", jobId, e);
            final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL,
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

            chunkItemEntities.entities.add(createItem(jobId, chunkId, itemCounter, itemState, null));
            chunkItemEntities.chunkStateChange.incFailed(1);
        }
        return chunkItemEntities;
    }

    /**
     * Updates item entities for given chunk
     * @param chunk external chunk
     * @return item entities compound object
     * @throws DuplicateChunkException if attempting to update already existing chunk
     * @throws InvalidInputException if unable to find referenced items or if external chunk belongs to PARTITIONING
     * phase
     * @throws JobStoreException
     */
    @Stopwatch
    ChunkItemEntities updateChunkItemEntities(ExternalChunk chunk) throws JobStoreException {

        Date nextItemBegin = new Date();

        final State.Phase phase = chunkTypeToStatePhase(chunk.getType());
        final ChunkItemEntities chunkItemEntities = new ChunkItemEntities();
        chunkItemEntities.chunkStateChange.setPhase(phase);

        final Iterator<ChunkItem> nextIterator = chunk.nextIterator();
        for (ChunkItem chunkItem : chunk) {
            final ItemEntity.Key itemKey = new ItemEntity.Key((int) chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId());
            final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
            if (itemEntity == null) {
                throwInvalidInputException(String.format("ItemEntity.%s could not be found", itemKey), JobError.Code.INVALID_ITEM_IDENTIFIER);
            }

            if (itemEntity.getState().phaseIsDone(phase)) {
                throwDuplicateChunkException(String.format("Aborted attempt to add item %s to already finished %s phase", itemEntity.getKey(), phase), JobError.Code.ILLEGAL_CHUNK);
            }

            chunkItemEntities.entities.add(itemEntity);

            final ItemData itemData = new ItemData(
                    StringUtil.base64encode(
                            StringUtil.asString(chunkItem.getData(), chunk.getEncoding()),
                            chunk.getEncoding()), chunk.getEncoding());

            final StateChange itemStateChange = new StateChange()
                    .setPhase(phase)
                    .setBeginDate(nextItemBegin)                                            // ToDo: ExternalChunk type must contain beginDate
                    .setEndDate(new Date());                                                // ToDo: ExternalChunk type must contain endDate

            setOutcomeOnItemEntityFromPhase(chunk, phase, itemEntity, itemData);
            if (nextIterator.hasNext()) {
                itemEntity.setNextProcessingOutcome(nextIterator.next());
            }

            setItemStateOnChunkItemFromStatus(chunkItemEntities, chunkItem, itemStateChange);

            final State itemState = updateItemEntityState(itemEntity, itemStateChange);
            if(itemState.allPhasesAreDone()) {
                itemEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
            }
            nextItemBegin = new Date();
        }
        return chunkItemEntities;
    }
    private void setOutcomeOnItemEntityFromPhase(ExternalChunk chunk, State.Phase phase, ItemEntity itemEntity, ItemData itemData) throws InvalidInputException {
        switch (phase) {
            case PROCESSING: itemEntity.setProcessingOutcome(itemData);
                break;
            case DELIVERING: itemEntity.setDeliveringOutcome(itemData);
                break;
            case PARTITIONING:
                throwInvalidInputException(String.format("Trying to add items to %s phase of Chunk[%d,%d]", phase, chunk.getJobId(), chunk.getChunkId()), JobError.Code.ILLEGAL_CHUNK);
        }
    }
    private void setItemStateOnChunkItemFromStatus(ChunkItemEntities chunkItemEntities, ChunkItem chunkItem, StateChange itemStateChange) {
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
     * Creates new item entity with data from the partitioning phase
     * @param jobId id of job containing chunk
     * @param chunkId id of chunk for which items are to be created
     * @param itemId id of item
     * @param state initial state of item
     * @param data result of the partitioning phase
     * @return created item entity (managed)
     */
    @Stopwatch
    ItemEntity createItem(int jobId, int chunkId, short itemId, State state, ItemData data) {
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(new ItemEntity.Key(jobId, chunkId, itemId));
        itemEntity.setState(state);
        itemEntity.setPartitioningOutcome(data);
        entityManager.persist(itemEntity);
        return itemEntity;
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
    SinkCacheEntity cacheSink(String sinkJson) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkJson, "sink");
        final Query storedProcedure = entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE);
        storedProcedure.setParameter("checksum", Md5.asHex(sinkJson.getBytes(StandardCharsets.UTF_8)));
        storedProcedure.setParameter("sink", new SinkConverter().convertToDatabaseColumn(sinkJson));
        return (SinkCacheEntity) storedProcedure.getSingleResult();
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
    FlowCacheEntity cacheFlow(String flowJson) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowJson, "flow");
        final Query storedProcedure = entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE);
        storedProcedure.setParameter("checksum", Md5.asHex(flowJson.getBytes(StandardCharsets.UTF_8)));
        storedProcedure.setParameter("flow", new FlowConverter().convertToDatabaseColumn(flowJson));
        return (FlowCacheEntity) storedProcedure.getSingleResult();
    }

    @Stopwatch
    public ItemData getItemData(int jobId, int chunkId, short itemId, State.Phase phase) throws InvalidInputException {
        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, key);
        if (itemEntity == null) {
            throwInvalidInputException(String.format("ItemEntity.Key{jobId:%d, chunkId:%d, itemId:%d} could not be found", jobId, chunkId, itemId), JobError.Code.INVALID_ITEM_IDENTIFIER);
        }
        switch(phase) {
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

    /*
     * private methods
     */

    private PgJobStore getProxyToSelf() {
        return sessionContext.getBusinessObject(PgJobStore.class);
    }

    private <T> T getExclusiveAccessFor(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE);
    }

    private State.Phase chunkTypeToStatePhase(ExternalChunk.Type chunkType) {
        switch (chunkType) {
            case PARTITIONED: return State.Phase.PARTITIONING;
            case PROCESSED:   return State.Phase.PROCESSING;
            case DELIVERED:   return State.Phase.DELIVERING;
            default: throw new IllegalStateException(String.format("Unknown type: '%s'", chunkType));
        }
    }

    private State updateJobEntityState(JobEntity jobEntity, StateChange stateChange) {
        final State jobState = new State(jobEntity.getState());
        jobState.updateState(stateChange);
        jobEntity.setState(jobState);
        return jobState;
    }

    private State updateChunkEntityState(ChunkEntity chunkEntity, StateChange stateChange) {
        final State chunkState = new State(chunkEntity.getState());
        chunkState.updateState(stateChange);
        chunkEntity.setState(chunkState);
        return chunkState;
    }

    private State updateItemEntityState(ItemEntity itemEntity, StateChange stateChange) {
        final State itemState = new State(itemEntity.getState());
        itemState.updateState(stateChange);
        itemEntity.setState(itemState);
        return itemState;
    }

    private JobEntity abortJob(JobEntity jobEntity, List<Diagnostic> diagnostics) {
        final State jobState = new State(jobEntity.getState());
        jobState.getDiagnostics().addAll(diagnostics);
        jobEntity.setState(jobState);
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        return jobEntity;
    }

    private void throwDuplicateChunkException(String errMsg, JobError.Code jobErrorCode) throws DuplicateChunkException {
        final JobError jobError = new JobError(jobErrorCode, errMsg, JobError.NO_STACKTRACE);
        throw new DuplicateChunkException(errMsg, jobError);
    }
    private void throwInvalidInputException(String errMsg, JobError.Code jobErrorCode) throws InvalidInputException {
        final JobError jobError = new JobError(jobErrorCode, errMsg, JobError.NO_STACKTRACE);
        throw new InvalidInputException(errMsg, jobError);
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
            if (jobEntity.getTimeOfCreation() != null) {
                logPattern += " timeOfCreation={}";
                logArguments.add(jobEntity.getTimeOfCreation().getTime());
            }
            if (jobEntity.getTimeOfCompletion() != null) {
                logPattern += " timeOfCompletion={}";
                logArguments.add(jobEntity.getTimeOfCompletion().getTime());
            }
            LOGGER.info(logPattern, logArguments.toArray());
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