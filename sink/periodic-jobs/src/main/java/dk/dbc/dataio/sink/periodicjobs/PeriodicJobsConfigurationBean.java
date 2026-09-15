package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import jakarta.persistence.EntityManager;
import org.glassfish.jersey.internal.guava.Cache;
import org.glassfish.jersey.internal.guava.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class PeriodicJobsConfigurationBean {
    final Cache<Integer, PeriodicJobsDelivery> deliveryCache = CacheBuilder.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(1, TimeUnit.HOURS).build();

    FlowStoreServiceConnector flowStoreServiceConnector;
    JobStoreServiceConnector jobStoreServiceConnector;

    /**
     * Returns delivery configuration for given job
     *
     * @param chunkId id of the chunk the lookup is made for, which gates whether the
     *                delivery entity is persisted
     * @return delivery configuration as {@link PeriodicJobsDelivery}
     */
    public PeriodicJobsDelivery getDelivery(int jobId, int chunkId, EntityManager entityManager) {
        PeriodicJobsDelivery periodicJobsDelivery = deliveryCache.getIfPresent(jobId);
        if (periodicJobsDelivery != null) {
            // Return delivery entity from local bean cache.
            return periodicJobsDelivery;
        }
        periodicJobsDelivery = entityManager.find(PeriodicJobsDelivery.class, jobId);
        if (periodicJobsDelivery == null) {
            // Retrieve harvester config from flow-store and create new
            // delivery entity.
            PeriodicJobsHarvesterConfig periodicJobsHarvesterConfig = getHarvesterConfig(jobId);
            periodicJobsDelivery = new PeriodicJobsDelivery(jobId);
            periodicJobsDelivery.setConfig(periodicJobsHarvesterConfig);
        }
        if (chunkId == 0) {
            // Only allow the first chunk to persist the delivery entity
            entityManager.persist(periodicJobsDelivery);
        }
        updateDeliveryCache(jobId, periodicJobsDelivery);

        return periodicJobsDelivery;
    }

    private PeriodicJobsHarvesterConfig getHarvesterConfig(int jobId) {
        HarvesterToken harvesterToken = getHarvesterToken(jobId);
        try {
            return flowStoreServiceConnector
                    .getHarvesterConfig(harvesterToken.getId(), PeriodicJobsHarvesterConfig.class);
        } catch (RuntimeException | FlowStoreServiceConnectorException e) {
            throw new RuntimeException(
                    String.format("Failed to find harvester config for token %s", harvesterToken), e);
        }
    }

    private HarvesterToken getHarvesterToken(int jobId)  {
        try {
            JobListCriteria findJobCriteria = new JobListCriteria()
                    .where(new ListFilter<>(JobListCriteria.Field.JOB_ID,
                            ListFilter.Op.EQUAL, jobId));
            JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.listJobs(findJobCriteria).get(0);
            return HarvesterToken.of(jobInfoSnapshot.getSpecification().getAncestry().getHarvesterToken());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new RuntimeException(
                    String.format("Failed to find job %d", jobId), e);
        }
    }

    /* The LRU cache is not thread-safe in itself */
    private synchronized void updateDeliveryCache(Integer jobId, PeriodicJobsDelivery delivery) {
        deliveryCache.put(jobId, delivery);
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
