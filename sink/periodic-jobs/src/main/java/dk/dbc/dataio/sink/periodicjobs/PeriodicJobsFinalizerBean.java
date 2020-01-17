/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * It is the responsibility of this class to facilitate delivery of a periodic job
 */
@Stateless
public class PeriodicJobsFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsFinalizerBean.class);

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @EJB PeriodicJobsConfigurationBean periodicJobsConfigurationBean;

    @Timed
    public Chunk handleTerminationChunk(Chunk chunk) throws SinkException {
        LOGGER.info("Finalizing periodic job {}", chunk.getJobId());

        final Chunk result;
        final PeriodicJobsDelivery delivery = periodicJobsConfigurationBean.getDelivery(chunk);
        switch (delivery.getConfig().getContent().getPickupType()) {
            case HTTP:
                // TODO: 17/01/2020 Implement HTTP specific finalizer
                result = null;
                break;
            default:
                result = getUnhandledPickupTypeResult(chunk, delivery.getConfig().getContent().getPickupType());
        }

        LOGGER.info("Deleted {} data blocks for job {}",
                deleteDataBlocks(delivery.getJobId()), delivery.getJobId());
        LOGGER.info("Deleted {} delivery entry for job {}",
                deleteDelivery(delivery.getJobId()), delivery.getJobId());

        return result;
    }

    private int deleteDataBlocks(Integer jobId) {
        return entityManager
                .createNamedQuery(PeriodicJobsDataBlock.DELETE_DATA_BLOCKS_QUERY_NAME)
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    private int deleteDelivery(Integer jobId) {
        return entityManager
                .createNamedQuery(PeriodicJobsDelivery.DELETE_DELIVERY_QUERY_NAME)
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    private Chunk getUnhandledPickupTypeResult(Chunk chunk, PeriodicJobsHarvesterConfig.PickupType pickupType) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        result.insertItem(
                ChunkItem.failedChunkItem()
                        .withData("Unhandled pickup type: " + pickupType));
        return result;
    }
}
