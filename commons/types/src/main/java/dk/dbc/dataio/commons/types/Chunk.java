package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunk DTO class.
 */
public class Chunk implements Serializable {
    public static final int MAX_RECORDS_PER_CHUNK = 10;
    static /* final */ long CHUNKID_LOWER_THRESHOLD = 0L;
    private static final long serialVersionUID = -6317006704089913073L;

    private /* final */ List<String> records;
    private /* final */ long id;
    private /* final */ Flow flow;

    private Chunk() {
        // JSON Unmarshalling of '{}' will trigger default constructor
        // causing getRecords() methods to throw NullPointerException
        // unless we set reasonable defaults.
        records = new ArrayList<String>(0);
    }

    public Chunk(long id, Flow flow) {
        this(id, flow, new ArrayList<String>(MAX_RECORDS_PER_CHUNK));
    }

    public Chunk(long id, Flow flow, List<String> records) throws NullPointerException, IllegalArgumentException {
        this.id = InvariantUtil.checkAboveThresholdOrThrow(id, "id", CHUNKID_LOWER_THRESHOLD);
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

    public long getId() {
        return id;
    }

    public Flow getFlow() {
        return flow;
    }

    public List<String> getRecords() {
        return new ArrayList<String>(records);
    }
}
