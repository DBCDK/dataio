package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

 /**
 * ProcessChunkResult DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
class ProcessChunkResult implements Serializable {
    private static final long serialVersionUID = -8494583387561924223L;

    private /* final */ List<String> results;
    private /* final */ long id;

    private ProcessChunkResult() { }
    
    ProcessChunkResult(long id, List<String> results) {
        this.id = id;
        this.results = new ArrayList<String>(results);
    }

    public List<String> getResults() {
        return new ArrayList<String>(results);
    }

    public long getId() {
        return id;
    }
}
