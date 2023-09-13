package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.harvester.types.Pickup;
import dk.dbc.dataio.harvester.types.SFtpPickup;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsFtpFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsHttpFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsMailFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsSFtpFinalizerBean;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is the responsibility of this class to facilitate delivery of a periodic job
 */
public class PeriodicJobsFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsFinalizerBean.class);

    EntityManager entityManager;
    PeriodicJobsConfigurationBean periodicJobsConfigurationBean;
    PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean;
    PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean;
    PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean;
    PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean;

    public Chunk handleTerminationChunk(Chunk chunk) throws InvalidMessageException {
        LOGGER.info("Finalizing periodic job {}", chunk.getJobId());

        PeriodicJobsDelivery delivery = periodicJobsConfigurationBean.getDelivery(chunk);
        Pickup pickup = delivery.getConfig().getContent().getPickup();
        Chunk result;

        if (pickup instanceof HttpPickup) {
            result = periodicJobsHttpFinalizerBean.deliver(chunk, delivery);
        } else if (pickup instanceof MailPickup) {
            result = periodicJobsMailFinalizerBean.deliver(chunk, delivery);
        } else if (pickup instanceof FtpPickup) {
            result = periodicJobsFtpFinalizerBean.deliver(chunk, delivery);
        } else if (pickup instanceof SFtpPickup) {
            result = periodicJobsSFtpFinalizerBean.deliver(chunk, delivery);
        } else {
            result = getUnhandledPickupTypeResult(chunk, pickup);
        }

        LOGGER.info("Deleted {} data blocks for job {}",
                deleteDataBlocks(delivery.getJobId()), delivery.getJobId());
        LOGGER.info("Deleted {} delivery entry for job {}",
                deleteDelivery(delivery.getJobId()), delivery.getJobId());
        return result;
    }

    public int deleteDataBlocks(Integer jobId) {
        return entityManager
                .createNamedQuery(PeriodicJobsDataBlock.DELETE_DATA_BLOCKS_QUERY_NAME)
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    public int deleteDelivery(Integer jobId) {
        return entityManager
                .createNamedQuery(PeriodicJobsDelivery.DELETE_DELIVERY_QUERY_NAME)
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    private Chunk getUnhandledPickupTypeResult(Chunk chunk, Pickup pickupType) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        result.insertItem(
                ChunkItem.failedChunkItem()
                        .withType(ChunkItem.Type.JOB_END)
                        .withData("Unhandled pickup type: " + pickupType));
        return result;
    }
    public PeriodicJobsFinalizerBean withPeriodicJobsConfigurationBean(PeriodicJobsConfigurationBean periodicJobsConfigurationBean) {
        this.periodicJobsConfigurationBean = periodicJobsConfigurationBean;
        return this;
    }

    public PeriodicJobsFinalizerBean withPeriodicJobsHttpFinalizerBean(PeriodicJobsHttpFinalizerBean periodicJobsHttpFinalizerBean) {
        this.periodicJobsHttpFinalizerBean = periodicJobsHttpFinalizerBean;
        return this;
    }

    public PeriodicJobsFinalizerBean withPeriodicJobsMailFinalizerBean(PeriodicJobsMailFinalizerBean periodicJobsMailFinalizerBean) {
        this.periodicJobsMailFinalizerBean = periodicJobsMailFinalizerBean;
        return this;
    }

    public PeriodicJobsFinalizerBean withPeriodicJobsFtpFinalizerBean(PeriodicJobsFtpFinalizerBean periodicJobsFtpFinalizerBean) {
        this.periodicJobsFtpFinalizerBean = periodicJobsFtpFinalizerBean;
        return this;
    }
    public PeriodicJobsFinalizerBean withPeriodicJobsSFtpFinalizerBean(PeriodicJobsSFtpFinalizerBean periodicJobsSFtpFinalizerBean) {
        this.periodicJobsSFtpFinalizerBean = periodicJobsSFtpFinalizerBean;
        return this;
    }

    public PeriodicJobsFinalizerBean withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }
}
