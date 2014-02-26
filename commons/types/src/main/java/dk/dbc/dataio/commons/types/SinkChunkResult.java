package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

 /**
 * SinkChunkResult DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class SinkChunkResult extends AbstractChunk implements Serializable {
    private static final long serialVersionUID = -8494583387561924223L;

    private /* final */ String encoding;

    private SinkChunkResult() {
        // JSON Unmarshalling of '{}' will trigger default constructor
        // causing getEncoding() and getItems() methods to throw NullPointerException
        // unless we set reasonable defaults.
        encoding = "DEFAULT";
        items = new ArrayList<ChunkItem>(0);
    }

    public SinkChunkResult(long jobId, long chunkId, Charset encoding, List<ChunkItem> items) {
        this.jobId = InvariantUtil.checkAboveThresholdOrThrow(jobId, "jobId", Constants.JOB_ID_LOWER_BOUND);
        this.chunkId = InvariantUtil.checkAboveThresholdOrThrow(chunkId, "chunkId", Constants.CHUNK_ID_LOWER_BOUND);
        this.encoding = encoding.name();
        this.items = new ArrayList<ChunkItem>(items);
    }

    public Charset getEncoding() {
        return Charset.forName(encoding);
    }
}
