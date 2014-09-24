package dk.dbc.dataio.commons.types;

import java.util.List;

public class JobCompletionState {

    private /* final */ long jobId;
    private /* final */ List<ChunkCompletionState> chunks;

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
