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

    private /* final */ Flow flow;
    private /* final */ SupplementaryProcessData supplementaryProcessData;

    private Chunk() {
        // JSON Unmarshalling of '{}' will trigger default constructor
        // causing getItems() methods to throw NullPointerException
        // unless we set reasonable defaults.
        items = new ArrayList<ChunkItem>(0);
    }

    public Chunk(long jobId, long chunkId, Flow flow, SupplementaryProcessData supplementaryProcessData) {
        this(jobId, chunkId, flow, supplementaryProcessData, new ArrayList<ChunkItem>(MAX_RECORDS_PER_CHUNK));
    }

    public Chunk(long jobId, long chunkId, Flow flow, SupplementaryProcessData supplementaryProcessData, List<ChunkItem> records) throws NullPointerException, IllegalArgumentException {
        this.jobId = InvariantUtil.checkAboveThresholdOrThrow(jobId, "jobId", JOBID_LOWER_THRESHOLD);
        this.chunkId = InvariantUtil.checkAboveThresholdOrThrow(chunkId, "chunkId", CHUNKID_LOWER_THRESHOLD);
        this.flow = InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.supplementaryProcessData = InvariantUtil.checkNotNullOrThrow(supplementaryProcessData, "supplementaryProcessData");
        this.items = InvariantUtil.checkNotNullOrThrow(records, "records");
        if (this.items.size() > MAX_RECORDS_PER_CHUNK) {
            throw new IllegalArgumentException("Number of records exceeds MAX_RECORDS_PER_CHUNK");
        }
    }

    @Override
    public void addItem(ChunkItem item) {
        if (items.size() >= MAX_RECORDS_PER_CHUNK) {
            throw new IndexOutOfBoundsException();
        }
        super.addItem(item);
    }

    public Flow getFlow() {
        return flow;
    }

    public SupplementaryProcessData getSupplementaryProcessData() {
        return supplementaryProcessData;
    }
}
