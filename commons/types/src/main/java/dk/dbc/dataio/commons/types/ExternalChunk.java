package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chunk-type for using outside of the jobstore. Internally in the jobstore, a
 * chunk entity will be used.
 * <p>
 * This class can be viewed as a small extension/simplification to a list with {@link ChunkItem}s, with an invariant. The invariant is described in the method
 * {@link #insertItem}.
 * <p>
 * ChunkItems can be inserted with insertItem. Extraction is through an
 * iterator, since it is assumed that no one ever needs to access ChunkItems
 * directly inside this class, but always consecutively.
 */
public class ExternalChunk implements Iterable<ChunkItem> {

    public enum Type {

        PARTITIONED,
        PROCESSED,
        DELIVERED
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalChunk.class);

    private final Type type;
    private final long jobId;
    private final long chunkId;
    @JsonProperty
    private final List<ChunkItem> items;

    /**
     * @param jobId cannot be negative.
     * @param chunkId cannot be negative.
     * @param type
     */
    public ExternalChunk(long jobId, long chunkId, Type type) {
        if (jobId < 0 || chunkId < 0) {
            String msg = String.format("Neither JobId nor ChunkId can be negative: [%d/%d]", jobId, chunkId);
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.type = type;
        this.items = new ArrayList<>();
    }

    // Private constructor for JsonUtil.fromJson().
    // This constructor uses insertItem to ensure that the invariant for the object is upheld.
    @JsonCreator
    private ExternalChunk(@JsonProperty("jobId") long jobId,
            @JsonProperty("chunkId") long chunkId,
            @JsonProperty("type") Type type,
            @JsonProperty("items") List<ChunkItem> items) {
        this(jobId, chunkId, type);
        // ensure to uphold invariant
        for (ChunkItem item : items) {
            insertItem(item);
        }
    }

    public long getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    public Type getType() {
        return type;
    }

    /**
     * Inserting ChunkItems into the Chunk.
     *
     * You must insert your ChunkItems consecutively. That is, the first ChunkItem must have id 0, the next id 1 and so forth.
     * If you insert a ChunkItem out of order, an IllegalArgumentException is thrown.
     *
     * @param item The ChunkItem to insert.
     */
    public void insertItem(ChunkItem item) throws IllegalArgumentException {
        if (item.getId() != items.size()) {
            String msg = String.format("ChunkItems must be inserted consecutively. Size of list: %d inserted item-id: %d", items.size(), item.getId());
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        items.add(item);
    }

    @Override
    public Iterator<ChunkItem> iterator() {
        return items.iterator();
    }
}
