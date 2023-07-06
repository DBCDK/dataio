package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import javax.persistence.EntityManager;

public class PeriodicJobsConfigurationBean {
    final Cache<Integer, PeriodicJobsDelivery> deliveryCache = CacheManager.createLRUCache(10);

    EntityManager entityManager;

    FlowStoreServiceConnector flowStoreServiceConnector;
    JobStoreServiceConnector jobStoreServiceConnector;

    /**
     * Returns delivery configuration for given chunk
     *
     * @param chunk {@link Chunk} for which get delivery configuration
     * @return delivery configuration as {@link PeriodicJobsDelivery}
     */
    public PeriodicJobsDelivery getDelivery(Chunk chunk) {
        Integer jobId = Math.toIntExact(chunk.getJobId());
        PeriodicJobsDelivery periodicJobsDelivery = deliveryCache.get(jobId);
        if (periodicJobsDelivery != null) {
            // Return delivery entity from local bean cache.
            return periodicJobsDelivery;
        }
        periodicJobsDelivery = entityManager.find(PeriodicJobsDelivery.class, jobId);
        if (periodicJobsDelivery == null) {
            // Retrieve harvester config from flow-store and create new
            // delivery entity.
            PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig = getHarvesterConfig(chunk);
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

    private PeriodicJobsHarvesterConfig getHarvesterConfig(Chunk chunk) {
        HarvesterToken harvesterToken = getHarvesterToken(chunk);
        try {
            return flowStoreServiceConnector
                    .getHarvesterConfig(harvesterToken.getId(), PeriodicJobsHarvesterConfig.class);
        } catch (RuntimeException | FlowStoreServiceConnectorException e) {
            throw new RuntimeException(
                    String.format("Failed to find harvester config for token %s", harvesterToken), e);
        }
    }

    private HarvesterToken getHarvesterToken(Chunk chunk)  {
        try {
            JobListCriteria findJobCriteria = new JobListCriteria()
                    .where(new ListFilter<>(JobListCriteria.Field.JOB_ID,
                            ListFilter.Op.EQUAL, chunk.getJobId()));
            JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.listJobs(findJobCriteria).get(0);
            return HarvesterToken.of(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new RuntimeException(
                    String.format("Failed to find job %d", chunk.getJobId()), e);
        }
    }

    /* The LRU cache is not thread-safe in itself */
    private synchronized void updateDeliveryCache(Integer jobId, PeriodicJobsDelivery delivery) {
        deliveryCache.put(jobId, delivery);
    }

    public PeriodicJobsConfigurationBean withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    public PeriodicJobsConfigurationBean withFlowstoreConnector(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        return this;
    }

    public PeriodicJobsConfigurationBean withJobstoreConnector(JobStoreServiceConnector jobStoreServiceConnector) {
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        return this;
    }

}
