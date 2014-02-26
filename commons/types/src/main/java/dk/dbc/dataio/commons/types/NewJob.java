package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * NewJob DTO class. Used by the job-store to signal new job availability to the job-processor,
 */
public class NewJob implements Serializable {
    private static final long serialVersionUID = 1497533904953891370L;

    private final long jobId;
    private final long chunkCount;
    private final Sink sink;

    public NewJob(long jobId, long chunkCount, Sink sink) throws NullPointerException, IllegalArgumentException {
        this.jobId = InvariantUtil.checkLowerBoundOrThrow(jobId, "jobId", Constants.JOB_ID_LOWER_BOUND);
        this.chunkCount = InvariantUtil.checkLowerBoundOrThrow(chunkCount, "chunkCount", Constants.CHUNK_COUNT_LOWER_BOUND);
        this.sink = InvariantUtil.checkNotNullOrThrow(sink, "sink");
    }

    public long getJobId() {
        return jobId;
    }

    public long getChunkCount() {
        return chunkCount;
    }

    public Sink getSink() {
        return sink;
    }
}
