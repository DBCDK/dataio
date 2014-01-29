package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.Sink;

public class NewJobBuilder {
    private long jobId = 43L;
    private long chunkCount = 1L;
    private Sink sink = new SinkBuilder().build();

    public NewJobBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public NewJobBuilder setChunkCount(long chunkCount) {
        this.chunkCount = chunkCount;
        return this;
    }

    public NewJobBuilder setSink(Sink sink) {
        this.sink = sink;
        return this;
    }

    public NewJob build() {
        return new NewJob(this.jobId, this.chunkCount, this.sink);
    }
}
