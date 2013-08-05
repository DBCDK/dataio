package dk.dbc.dataio.engine;

import java.nio.file.Path;

public class Job {
    private final long id;
    private final Path originalDataPath;
    private final FlowInfo flowInfo;

    public Job(long id, Path originalDataPath, FlowInfo flowInfo) {
        this.id = id;
        this.originalDataPath = originalDataPath;
        this.flowInfo = flowInfo;
    }

    public long getId() {
        return id;
    }

    public FlowInfo getFlowInfo() {
        return flowInfo;
    }

    public Path getOriginalDataPath() {
        return originalDataPath;
    }
}
