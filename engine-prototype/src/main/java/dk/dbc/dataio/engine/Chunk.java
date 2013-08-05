package dk.dbc.dataio.engine;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int MAX_RECORDS_PER_CHUNK = 10;
    private static final Logger log = LoggerFactory.getLogger(Chunk.class);

    private final List<String> records = new ArrayList<>(MAX_RECORDS_PER_CHUNK);

    private final long id;
    private final FlowInfo flowInfo;
    private int positionCounter = 0;

    public Chunk(long id, FlowInfo flowInfo) {
        this.id = id;
        this.flowInfo = flowInfo;
    }
    
    void addRecord(String record) {
        if (positionCounter < MAX_RECORDS_PER_CHUNK) {
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

    public int getRecordCount() {
        return records.size();
    }

    public String toJson() {
        final StringWriter stringWriter = new StringWriter();
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(stringWriter, this);
        } catch (IOException e) {
            log.error("Exception caught when trying to marshall Chunk object {} to JSON", id, e);
        }
        return stringWriter.toString();
    }

    public static Chunk fromJson(String json) {
        Chunk chunk = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            chunk = objectMapper.readValue(json, Chunk.class);
        } catch (IOException e) {
            log.error("Exception caught when trying to unmarshall JSON {} to Chunk object", json, e);
        }
        return chunk;
    }
}
