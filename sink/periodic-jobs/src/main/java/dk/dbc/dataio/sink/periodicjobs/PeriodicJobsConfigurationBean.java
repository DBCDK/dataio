package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.sink.types.SinkException;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

@Singleton
public class PeriodicJobsConfigurationBean {
    final Cache<Integer, PeriodicJobsDelivery> deliveryCache = CacheManager.createLRUCache(10);

    @PersistenceUnit(unitName="periodic-jobs_PU")
    EntityManagerFactory entityManagerFactory;

    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    /**
     * Returns delivery configuration for given chunk
     * @param chunk {@link Chunk} for which get delivery configuration
     * @return delivery configuration as {@link PeriodicJobsDelivery}
     * @throws SinkException if unable to resolve delivery configuration for chunk
     */
    @Lock(LockType.READ)
    public PeriodicJobsDelivery getDelivery(Chunk chunk) throws SinkException {
        final Integer jobId = Math.toIntExact(chunk.getJobId());
        PeriodicJobsDelivery  periodicJobsDelivery = deliveryCache.get(jobId);
        if (periodicJobsDelivery != null) {
            // Return delivery entity from local bean cache.
            return periodicJobsDelivery;
        }
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        periodicJobsDelivery = entityManager.find(PeriodicJobsDelivery.class, jobId);
        if (periodicJobsDelivery == null) {
            // Retrieve harvester config from flow-store and create new
            // delivery entity.
            final PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig = getHarvesterConfig(chunk);
            periodicJobsDelivery = new PeriodicJobsDelivery(Math.toIntExact(chunk.getJobId()));
            periodicJobsDelivery.setConfig(periodicJobsHarvesterConfig);
        }
        if (chunk.getChunkId() == 0) {
            // Only allow the first chunk to persist the delivery entity
            entityManager.persist(periodicJobsDelivery);
        }
        updateDeliveryCache(jobId, periodicJobsDelivery);

        return periodicJobsDelivery;
    }

    private PeriodicJobsHarvesterConfig getHarvesterConfig(Chunk chunk) throws SinkException {
        final HarvesterToken harvesterToken = getHarvesterToken(chunk);
        try {
            return flowStoreServiceConnectorBean.getConnector()
                    .getHarvesterConfig(harvesterToken.getId(), PeriodicJobsHarvesterConfig.class);
        } catch (RuntimeException | FlowStoreServiceConnectorException e) {
            throw new SinkException(
                    String.format("Failed to find harvester config for token %s", harvesterToken), e);
        }
    }

    private HarvesterToken getHarvesterToken(Chunk chunk) throws SinkException {
        try {
            final JobListCriteria findJobCriteria = new JobListCriteria()
                    .where(new ListFilter<>(JobListCriteria.Field.JOB_ID,
                            ListFilter.Op.EQUAL, chunk.getJobId()));
            final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnectorBean.getConnector()
                    .listJobs(findJobCriteria).get(0);
            return HarvesterToken.of(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new SinkException(
                    String.format("Failed to find job %d", chunk.getJobId()), e);
        }
    }

    /* The LRU cache is not thread-safe in itself */
    private synchronized void updateDeliveryCache(Integer jobId, PeriodicJobsDelivery delivery) {
        deliveryCache.put(jobId, delivery);
    }
}
