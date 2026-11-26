package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStore;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class RemotePartitioning implements Serializable, Callable<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemotePartitioning.class);

    private final Sink sink;

    public RemotePartitioning(Sink sink) {
        this.sink = sink;
    }

    @Override
    public Void call() throws Exception {
        LOGGER.info("Remote partitioning request received for sink {}", sink.getId());
        InitialContext ctx = new InitialContext();
        ManagedExecutorService executorSvc = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedScheduledExecutorService");
        PgJobStore jobStore = (PgJobStore) ctx.lookup(PgJobStore.PG_JOB_STORE_JNDI);
        executorSvc.runAsync(() -> jobStore.partitionNextJobForSinkIfAvailable(sink))
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        LOGGER.error("Remote partitioning failed for sink {}", sink.getId(), failure);
                    }
                });
        return null;
    }

    @Override
    public String toString() {
        return "RemotePartitioning{sinkId=" + sink.getId() + "}";
    }
}
