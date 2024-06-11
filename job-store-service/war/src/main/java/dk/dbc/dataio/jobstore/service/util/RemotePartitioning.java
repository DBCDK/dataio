package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStore;
import jakarta.enterprise.concurrent.ManagedExecutorService;

import javax.naming.InitialContext;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class RemotePartitioning implements Serializable, Callable<Void> {
    private final Sink sink;

    public RemotePartitioning(Sink sink) {
        this.sink = sink;
    }

    @Override
    public Void call() throws Exception {
        InitialContext ctx = new InitialContext();
        ManagedExecutorService executorSvc = (ManagedExecutorService) ctx.lookup("java:comp/DefaultManagedScheduledExecutorService");
        PgJobStore jobStore = (PgJobStore) ctx.lookup("java:global/jobstore/PgJobStore");
        executorSvc.runAsync(() -> jobStore.partitionNextJobForSinkIfAvailable(sink));
        return null;
    }
}
