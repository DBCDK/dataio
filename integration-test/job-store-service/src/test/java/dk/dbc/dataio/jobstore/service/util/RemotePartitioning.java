package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Sink;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Client-side stand-in for the job-store task of the same name, which lives in the war and is not
 * on this module's classpath.
 * <p>
 * Only serialises. The job-store member deserialises it as its own class and runs that, so the
 * class name, {@code serialVersionUID} and fields must match the job-store original.
 */
public class RemotePartitioning implements Serializable, Callable<Void> {
    private static final long serialVersionUID = 2179284016524911313L;

    private final Sink sink;

    public RemotePartitioning(Sink sink) {
        this.sink = sink;
    }

    @Override
    public Void call() {
        throw new UnsupportedOperationException("runs only on the job-store member");
    }
}
