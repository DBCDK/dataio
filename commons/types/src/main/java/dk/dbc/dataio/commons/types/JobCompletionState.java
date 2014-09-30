package dk.dbc.dataio.commons.types;

import java.io.Serializable;
import java.util.List;

public class JobCompletionState implements Serializable {
    private static final long serialVersionUID = -4951902854312515109L;

    private /* final */ long jobId;
    private /* final */ List<ChunkCompletionState> chunks;

    private JobCompletionState() {}

    public JobCompletionState(long jobId, List<ChunkCompletionState> chunks) {
        this.jobId = jobId;
        this.chunks = chunks;
    }

    public long getJobId() {
        return jobId;
    }

    public List<ChunkCompletionState> getChunks() {
        return chunks;
    }
}
