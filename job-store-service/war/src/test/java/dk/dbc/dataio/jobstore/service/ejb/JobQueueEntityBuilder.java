package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;

import java.sql.Timestamp;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;
import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter.XML;
import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State;
import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.State.IN_PROGRESS;

public class JobQueueEntityBuilder {

    private int id = 1;
    private Timestamp timeOfEntry = new Timestamp(System.currentTimeMillis());
    private int jobId = 10;
    private JobEntity job;
    private long sinkId = 100;
    private State state = IN_PROGRESS;
    private RecordSplitter recordSplitterType = XML;

    public JobQueueEntityBuilder setJob(JobEntity job) {
        this.job = job;
        return this;
    }

    public JobQueueEntityBuilder setSinkId(long sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public JobQueueEntityBuilder setState(State state) {
        this.state = state;
        return this;
    }

    public JobQueueEntityBuilder setRecordSplittertype(RecordSplitter recordSplitterType) {
        this.recordSplitterType = recordSplitterType;
        return this;
    }

    public JobQueueEntity build() {
        JobQueueEntity jobQueueEntity = new JobQueueEntity();
        jobQueueEntity.setJob(this.job);
        jobQueueEntity.setSinkId(this.sinkId);
        jobQueueEntity.setState(this.state);
        jobQueueEntity.setRecordSplitterType(this.recordSplitterType);
        return jobQueueEntity;
    }
}
