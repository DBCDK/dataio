package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunk DTO class.
 */
public class Chunk extends AbstractChunk implements Serializable {
    public static final int MAX_RECORDS_PER_CHUNK = 10;
    static /* final */ long JOBID_LOWER_THRESHOLD = 0L;
    static /* final */ long CHUNKID_LOWER_THRESHOLD = 0L;
    private static final long serialVersionUID = -6317006704089913073L;

    private /* final */ long jobId;
    private /* final */ long chunkId;
    private /* final */ List<String> records;
    private /* final */ Flow flow;

    private Chunk() {
        // JSON Unmarshalling of '{}' will trigger default constructor
        // causing getRecords() methods to throw NullPointerException
        // unless we set reasonable defaults.
        records = new ArrayList<String>(0);
    }

    public Chunk(long jobId, long chunkId, Flow flow) {
        this(jobId, chunkId, flow, new ArrayList<String>(MAX_RECORDS_PER_CHUNK));
    }

    public Chunk(long jobId, long chunkId, Flow flow, List<String> records) throws NullPointerException, IllegalArgumentException {
        this.jobId = InvariantUtil.checkAboveThresholdOrThrow(jobId, "jobId", JOBID_LOWER_THRESHOLD);
        this.chunkId = InvariantUtil.checkAboveThresholdOrThrow(chunkId, "chunkId", CHUNKID_LOWER_THRESHOLD);
        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.records = InvariantUtil.checkNotNullOrThrow(records, "records");
        if (this.records.size() > MAX_RECORDS_PER_CHUNK) {
            throw new IllegalArgumentException("Number of records exceeds MAX_RECORDS_PER_CHUNK");
        }
    }

    public void addRecord(String record) {
        if (records.size() < MAX_RECORDS_PER_CHUNK) {
            records.add(record);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public long getJobId() {
        return jobId;
    }

    @Override
    public long getChunkId() {
        return chunkId;
    }

    public Flow getFlow() {
        return flow;
    }

    public List<String> getRecords() {
        return new ArrayList<String>(records);
    }
}
