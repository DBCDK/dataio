package dk.dbc.dataio.commons.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

 /**
 * ChunkResult DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class ChunkResult implements Serializable {
    private static final long serialVersionUID = -8494583387561924223L;

    private /* final */ List<String> results;
    private /* final */ long id;

    private ChunkResult() { }

    public ChunkResult(long id, List<String> results) {
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
