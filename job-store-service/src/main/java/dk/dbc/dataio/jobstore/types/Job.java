package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Job DTO class.
 */
public class Job implements Serializable {
    static final long ID_LOWER_THRESHOLD = 0;
    private static final long serialVersionUID = 592111006810833332L;

    private final long id;
    private final Path originalDataPath;
    private final Flow flow;

    public Job(long id, Path originalDataPath, Flow flow) {
        this.id = InvariantUtil.checkAboveThresholdOrThrow(id, "id", ID_LOWER_THRESHOLD);
        this.originalDataPath = InvariantUtil.checkNotNullOrThrow(originalDataPath, "originalDataPath");
        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
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