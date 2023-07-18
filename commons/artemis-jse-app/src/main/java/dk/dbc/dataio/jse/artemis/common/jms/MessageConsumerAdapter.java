package dk.dbc.dataio.jse.artemis.common.jms;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jse.artemis.common.JobProcessorException;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.jse.artemis.common.service.ZombieWatch;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;

public abstract class MessageConsumerAdapter implements MessageConsumer {
    protected final ZombieWatch zombieWatch;
    protected final JobStoreServiceConnector jobStoreServiceConnector;

    public MessageConsumerAdapter(ServiceHub serviceHub) {
        zombieWatch = serviceHub.zombieWatch;
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        initMetrics(PrometheusMetricRegistry.create());
    }

    @Override
    public ZombieWatch getZombieWatch() {
        return zombieWatch;
    }

    protected void sendResultToJobStore(Chunk chunk) throws JobProcessorException {
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new JobProcessorException("Error while sending result to job-store", e);
        }
    }
}
