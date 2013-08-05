package dk.dbc.dataio.engine;

import java.util.List;

class ProcessChunkResult {

    private final List<String> results;
    private final long id;
    
    ProcessChunkResult(long id, List<String> results) {
        this.id = id;
        this.results = results;
    }

}
