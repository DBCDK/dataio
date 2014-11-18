package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Chunk DTO class.
 */
public class Chunk extends AbstractChunk implements Serializable {
    private static final long serialVersionUID = -6317006704089913073L;

    private /* final */ Flow flow;
    private /* final */ SupplementaryProcessData supplementaryProcessData;
    private /* final */ Set<String> keys;

    private Chunk() {
        // JSON Unmarshalling of '{}' will trigger default constructor
        // causing getItems()/getKeys() methods to throw NullPointerException
        // unless we set reasonable defaults.
        items = new ArrayList<ChunkItem>(0);
        keys = new HashSet<>(0);
    }

    public Chunk(long jobId, long chunkId, Flow flow, SupplementaryProcessData supplementaryProcessData) {
        this(jobId, chunkId, flow, supplementaryProcessData, new ArrayList<ChunkItem>(Constants.CHUNK_RECORD_COUNT_UPPER_BOUND));
    }

    public Chunk(long jobId, long chunkId, Flow flow, SupplementaryProcessData supplementaryProcessData, List<ChunkItem> records) throws NullPointerException, IllegalArgumentException {
        this.jobId = InvariantUtil.checkLowerBoundOrThrow(jobId, "jobId", Constants.JOB_ID_LOWER_BOUND);
        this.chunkId = InvariantUtil.checkLowerBoundOrThrow(chunkId, "chunkId", Constants.CHUNK_ID_LOWER_BOUND);
        this.flow = flow; // InvariantUtil.checkNotNullOrThrow(flow, "flow");
        this.supplementaryProcessData = InvariantUtil.checkNotNullOrThrow(supplementaryProcessData, "supplementaryProcessData");
        this.items = InvariantUtil.checkNotNullOrThrow(records, "records");
        if (this.items.size() > Constants.CHUNK_RECORD_COUNT_UPPER_BOUND) {
            throw new IllegalArgumentException("Number of records exceeds CHUNK_RECORD_COUNT_UPPER_BOUND");
        }
        keys = new HashSet<>();
    }

    @Override
    public void addItem(ChunkItem item) {
        if (items.size() >= Constants.CHUNK_RECORD_COUNT_UPPER_BOUND) {
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

    public void addKey(String key) {
        keys.add(key);
    }

    public Set<String> getKeys() {
        return new HashSet<>(keys);
    }
}
