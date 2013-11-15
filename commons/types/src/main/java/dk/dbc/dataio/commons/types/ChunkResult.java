package dk.dbc.dataio.commons.types;

import java.io.Serializable;
import java.nio.charset.Charset;
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

    private /* final */ long jobId;
    private /* final */ long chunkId;
    private /* final */ String encoding;
    private /* final */ List<String> results;

    private ChunkResult() { }

    public ChunkResult(long jobId, long chunkId, Charset encoding, List<String> results) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.encoding = encoding.name();
        this.results = new ArrayList<String>(results);
    }

    public long getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    public Charset getEncoding() {
        return Charset.forName(encoding);
    }

    public List<String> getResults() {
        return new ArrayList<String>(results);
    }
}
