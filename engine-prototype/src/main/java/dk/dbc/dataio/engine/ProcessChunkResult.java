package dk.dbc.dataio.engine;

import java.io.Serializable;
import java.util.List;

 /**
 * ProcessChunkResult DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields.
 */
class ProcessChunkResult implements Serializable {
    private static final long serialVersionUID = -8494583387561924223L;

    private /* final */ List<String> results;
    private /* final */ long id;
    
    ProcessChunkResult(long id, List<String> results) {
        this.id = id;
        this.results = results;
    }

    public List<String> getResults() {
        return results;
    }

    public long getId() {
        return id;
    }
}
