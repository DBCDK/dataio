package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Job DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 *
 * ToDo: Needs to be refactored if to be used directly in GWT GUI since we cannot use the Java7 Path construct.
 */
public class Job implements Serializable {
    private static final long serialVersionUID = 592111006810833332L;

    private /* final */ long id;
    private /* final */ Path originalDataPath;
    private /* final */ Flow flow;

    private Job() { }

    public Job(long id, Path originalDataPath, Flow flow) {
        this.id = id;
        this.originalDataPath = originalDataPath;
        this.flow = flow;
    }

    public long getId() {
        return id;
    }

    public Flow getFlow() {
        return flow;
    }

    public Path getOriginalDataPath() {
        return originalDataPath;
    }
    
}