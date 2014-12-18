package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.digest.Md5;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowConverter;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkConverter;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.nio.charset.StandardCharsets;

/**
 * This stateless Enterprise Java Bean (EJB) facilitates access to the job-store database through persistence layer
 */
@Stateless
public class PgJobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStore.class);

    @EJB
    JSONBBean jsonbBean;

    @PersistenceContext(unitName = "jobstorePU")
    EntityManager entityManager;

    /**
     * Adds Sink instance to job-store cache if not already cached
     * @param sink Sink object to cache
     * @return id of cache line
     * @throws NullPointerException if given null-valued sink
     * @throws IllegalStateException if unable to create checksum digest
     * @throws JobStoreException on failure to marshall
     * entity object to JSON
     */
    public SinkCacheEntity cacheSink(Sink sink) throws NullPointerException, IllegalStateException, JobStoreException {
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
    public FlowCacheEntity cacheFlow(Flow flow) throws NullPointerException, IllegalStateException, JobStoreException {
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
     * Adds item to job-store
     * <p>
     * Note that the timeOfCreation and timeOfLastModification fields will be set
     * automatically by the underlying database.
     * </p>
     * @param item ItemEntity instance to be persisted
     * @return managed JobEntity instance
     * @throws NullPointerException if given null-valued item
     */
    public ItemEntity addItem(ItemEntity item) {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(item, "item");
            entityManager.persist(item);
            entityManager.flush();
            entityManager.refresh(item);
            return item;
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds chunk to job-store
     * <p>
     * Note that the timeOfCreation and timeOfLastModification fields will be set
     * automatically by the underlying database.
     * </p>
     * @param chunk ChunkEntity instance to be persisted
     * @return managed ChunkEntity instance
     * @throws NullPointerException if given null-valued chunk
     */
    public ChunkEntity addChunk(ChunkEntity chunk) {
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
     * Adds job to job-store
     * <p>
     * Note that the id, timeOfCreation and timeOfLastModification fields will be set
     * automatically by the underlying database.
     * </p>
     * @param job JobEntity instance to be persisted
     * @return managed JobEntity instance
     * @throws NullPointerException if given null-valued job
     */
    public JobEntity addJob(JobEntity job) {
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
}
