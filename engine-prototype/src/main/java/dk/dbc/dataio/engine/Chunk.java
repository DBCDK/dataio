package dk.dbc.dataio.engine;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int MAX_RECORDS_PER_CHUNK = 10;
    private static final Logger log = LoggerFactory.getLogger(Chunk.class);

    private final List<String> records;

    private final long id;
    private final FlowInfo flowInfo;

    public Chunk(long id, FlowInfo flowInfo) {
        this(id, flowInfo, new ArrayList<String>(MAX_RECORDS_PER_CHUNK));
    }

    Chunk(long id, FlowInfo flowInfo, List<String> records) {
        if (records.size() > MAX_RECORDS_PER_CHUNK) {
            throw new IllegalArgumentException("Number of records exceeds MAX_RECORDS_PER_CHUNK");
        }
        this.id = id;
        this.flowInfo = flowInfo;
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

    public FlowInfo getFlowInfo() {
        return flowInfo;
    }

    public List<String> getRecords() {
        return records;
    }

    @JsonCreator
    public static Chunk createChunk(@JsonProperty("id") long id, @JsonProperty("flowInfo") FlowInfo flowInfo, @JsonProperty("records") List<String> records) {
        return new Chunk(id, flowInfo, records);
    }
}
