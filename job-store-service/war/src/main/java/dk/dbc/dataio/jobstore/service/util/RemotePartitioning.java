package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class RemotePartitioning implements Serializable, Callable<Void> {
    /* Fixed, so the copy of this class in integration-test/job-store-service, which submits it
       through a Hazelcast client, deserialises as this class. Change both together. */
    private static final long serialVersionUID = 2179284016524911313L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RemotePartitioning.class);

    private final Sink sink;

    public RemotePartitioning(Sink sink) {
        this.sink = sink;
    }

    /**
     * Starts partitioning of the next job for the sink, on the member this task runs on.
     * <p>
     * The task runs on a Hazelcast executor thread, which belongs to no application component.
     * Payara's managed executors refuse submissions from such a thread, so the work goes through
     * the asynchronous {@code PgJobStore} business method, which the EJB container runs inside the
     * application. The call returns as soon as the container has accepted it.
     *
     * @return nothing
     * @throws Exception if {@code PgJobStore} cannot be looked up or the asynchronous call is refused
     */
    @Override
    public Void call() throws Exception {
        LOGGER.info("Remote partitioning request received for sink {}", sink.getId());
        try {
            PgJobStore jobStore = (PgJobStore) new InitialContext().lookup(PgJobStore.PG_JOB_STORE_JNDI);
            jobStore.partitionNextJobForSinkIfAvailable(sink);
        } catch (Exception e) {
            LOGGER.error("Remote partitioning failed for sink {}", sink.getId(), e);
            throw e;
        }
        return null;
    }

    @Override
    public String toString() {
        return "RemotePartitioning{sinkId=" + sink.getId() + "}";
    }
}
