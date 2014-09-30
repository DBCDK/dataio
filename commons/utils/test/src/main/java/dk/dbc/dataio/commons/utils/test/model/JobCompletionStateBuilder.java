package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkCompletionState;
import dk.dbc.dataio.commons.types.JobCompletionState;

import java.util.ArrayList;
import java.util.List;

public class JobCompletionStateBuilder {
    public static long DEFAULT_JOB_ID = 283L;

    private long jobId = DEFAULT_JOB_ID;
    private List<ChunkCompletionState> chunks = new ArrayList<ChunkCompletionState>();

    public JobCompletionStateBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobCompletionStateBuilder addChunk(ChunkCompletionState chunk) {
        chunks.add(chunk);
        return this;
    }

    public JobCompletionState build() {
        final JobCompletionState jobCompletionState = new JobCompletionState(jobId, chunks);
        return jobCompletionState;
    }
}
