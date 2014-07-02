package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This Enterprise Java Bean (EJB) is responsible for chunk scheduling
 * via sequence analysis and for notifying the job processor about chunks
 * ready for processing.
 */
@Singleton
@Startup
public class JobSchedulerBean {
    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void scheduleChunk
            (Chunk chunk) throws JobStoreException {
        jobProcessorMessageProducer.send(chunk);
    }
}
