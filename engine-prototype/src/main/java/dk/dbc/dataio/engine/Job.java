package dk.dbc.dataio.engine;

import java.nio.file.Path;

public class Job {
    private final long id;
    private final Path originalDataPath;
    
    public Job(long id, Path originalDataPath) {
        this.id = id;
        this.originalDataPath = originalDataPath;
    }

    public long getId() {
        return id;
    }

    public FlowInfo getFlowInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Path getOriginalDataPath() {
        return originalDataPath;
    }
}
