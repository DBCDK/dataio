package dk.dbc.dataio.engine;

import java.util.ArrayList;
import java.util.List;

public class Chunk {

    public static final int MAX_RECORDS_PER_CHUNK = 10; 
    private final List<String> chunks = new ArrayList<>(MAX_RECORDS_PER_CHUNK);
    private int positionCounter = 0;
    
    void addRecord(String record) {
        if(positionCounter < MAX_RECORDS_PER_CHUNK) {
            chunks.add(record);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }
}
