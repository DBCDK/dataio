package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Chunk-type for using outside of the job-store. Internally in the job-store, a
 * chunk entity will be used.
 * <p>
 * This class can be viewed as a small extension/simplification to a list with
 * {@link ChunkItem}s, with the following invariant: items must be inserted
 * consecutively, ie. the first item must have id 0, the next id 1 and so forth.
 * Out of order insertions will cause an IllegalArgumentException to be thrown.
 * <p>
 * ChunkItems can be inserted with the insertItem() and addAllItems() methods.
 * Extraction is through an iterator, since it is assumed that no one ever needs
 * to access items directly inside this class, but always consecutively.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chunk implements Iterable<ChunkItem> {
    public enum Type {
        PARTITIONED,
        PROCESSED,
        DELIVERED
    }

    private final Type type;
    private final int jobId;
    private final long chunkId;
    @JsonProperty
    private final List<ChunkItem> items;

    /**
     * @param jobId   cannot be negative.
     * @param chunkId cannot be negative.
     * @param type    of job (PARTITIONED, PROCESSED, DELIVERED)
     */
    public Chunk(int jobId, long chunkId, Type type) {
        if (jobId < 0 || chunkId < 0) {
            throw new IllegalArgumentException(String.format("Neither job ID nor chunk ID can be negative: [%d/%d]",
                    jobId, chunkId));
        }
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.type = type;
        this.items = new ArrayList<>();
    }

    // Private constructor for JsonUtil.fromJson().
    // This constructor uses insertItem to ensure that the invariant for the object is upheld.
    @JsonCreator
    private Chunk(@JsonProperty("jobId") int jobId,
                  @JsonProperty("chunkId") long chunkId,
                  @JsonProperty("type") Type type,
                  @JsonProperty("items") List<ChunkItem> items) {
        this(jobId, chunkId, type);
        // ensure to uphold invariant
        addAllItems(items);
    }

    public int getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    public Type getType() {
        return type;
    }

    public int size() {
        return items.size();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @JsonIgnore
    public boolean isTerminationChunk() {
        return items.size() == 1
                && items.get(0).isTyped()
                && items.get(0).getType().get(0) == ChunkItem.Type.JOB_END;
    }

    public void addAllItems(List<ChunkItem> items) throws IllegalArgumentException {
        for (ChunkItem item : items) {
            insertItem(item);
        }
    }

    public void insertItem(ChunkItem item) throws IllegalArgumentException {
        if (item == null) {
            throw new IllegalArgumentException("item can not be null");
        }
        if (item.getId() != items.size()) {
            throw new IllegalArgumentException(String.format("ChunkItems must be inserted consecutively. Size of list: %d inserted item-id: %d",
                    items.size(), item.getId()));
        }
        items.add(item);
    }

    public List<ChunkItem> getItems() {
        return items;
    }

    @JsonIgnore
    public String getTrackingId() {
        return jobId + "/" + chunkId;
    }

    @Override
    public Iterator<ChunkItem> iterator() {
        return items.iterator();
    }

    @Override
    public String toString() {
        return "Chunk " + getTrackingId();
    }
}
