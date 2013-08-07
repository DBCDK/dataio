package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Chunk DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class Chunk implements Serializable {
    public static final int MAX_RECORDS_PER_CHUNK = 10;
    private static final long serialVersionUID = -6317006704089913073L;

    private /* final */ List<String> records;
    private /* final */ long id;
    private /* final */ Flow flow;

    private Chunk() { }

    public Chunk(long id, Flow flow) {
        this(id, flow, new ArrayList<String>(MAX_RECORDS_PER_CHUNK));
    }

    Chunk(long id, Flow flow, List<String> records) {
        if (records.size() > MAX_RECORDS_PER_CHUNK) {
            throw new IllegalArgumentException("Number of records exceeds MAX_RECORDS_PER_CHUNK");
        }
        this.id = id;
        this.flow = flow;
        this.records = records;
    }
    
    void addRecord(String record) {
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
        return records;
    }
}
