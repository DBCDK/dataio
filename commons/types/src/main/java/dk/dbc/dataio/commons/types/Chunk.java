package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.Charset;
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
    @JsonProperty
    private final List<ChunkItem> next;
    @JsonProperty
    private String encoding;

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
        this.next = new ArrayList<>(0);
        this.encoding = "UTF-8";
    }

    // Private constructor for JsonUtil.fromJson().
    // This constructor uses insertItem to ensure that the invariant for the object is upheld.
    @JsonCreator
    private Chunk(@JsonProperty("jobId") int jobId,
                  @JsonProperty("chunkId") long chunkId,
                  @JsonProperty("type") Type type,
                  @JsonProperty("items") List<ChunkItem> items,
                  @JsonProperty("next") List<ChunkItem> next) {
        this(jobId, chunkId, type);
        // ensure to uphold invariant
        addAllItems(items, next);
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
    public boolean hasNextItems() {
        return !next.isEmpty();
    }

    @JsonIgnore
    public boolean isTerminationChunk() {
        return items.size() == 1
                && items.get(0).isTyped()
                && items.get(0).getType().get(0) == ChunkItem.Type.JOB_END;
    }

    public Charset getEncoding() {
        return Charset.forName(encoding);
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding.name();
    }

    public void addAllItems(List<ChunkItem> items) throws IllegalArgumentException {
        for (ChunkItem item : items) {
            insertItem(item);
        }
    }

    public void addAllItems(List<ChunkItem> current, List<ChunkItem> next) throws IllegalArgumentException {
        if (next == null || next.isEmpty()) {
            addAllItems(current);
        } else {
            if (current.size() != next.size())
                throw new IllegalArgumentException(String.format("Size of current list %d differs from size of next list %s",
                        current.size(), next.size()));
            final Iterator<ChunkItem> currentIterator = current.iterator();
            final Iterator<ChunkItem> nextIterator = next.iterator();
            while (currentIterator.hasNext()) {
                ChunkItem nextItem = ChunkItem.UNDEFINED;
                if (nextIterator.hasNext()) {
                    nextItem = nextIterator.next();
                }
                insertItem(currentIterator.next(), nextItem);
            }
        }
    }

    public void insertItem(ChunkItem item) throws IllegalArgumentException {
        if (item == ChunkItem.UNDEFINED) {
            throw new IllegalArgumentException("item can not be null");
        }
        insert(items, item);
    }

    public void insertItem(ChunkItem currentItem, ChunkItem nextItem) throws IllegalArgumentException {
        insertItem(currentItem);
        if (nextItem != ChunkItem.UNDEFINED) {
            if (currentItem.getId() != nextItem.getId()) {
                throw new IllegalArgumentException(String.format("Current item id %d differs from next item id %d",
                        currentItem.getId(), nextItem.getId()));
            }
            insert(next, nextItem);
        }
    }

    public List<ChunkItem> getItems() {
        return items;
    }

    public List<ChunkItem> getNext() {
        return next;
    }

    @JsonIgnore
    public String getTrackingId() {
        return jobId + "/" + chunkId;
    }

    @Override
    public Iterator<ChunkItem> iterator() {
        return items.iterator();
    }

    public Iterator<ChunkItem> nextIterator() {
        return next.iterator();
    }

    private void insert(List<ChunkItem> collection, ChunkItem item) throws IllegalArgumentException {
        if (item.getId() != collection.size()) {
            throw new IllegalArgumentException(String.format("ChunkItems must be inserted consecutively. Size of list: %d inserted item-id: %d",
                    collection.size(), item.getId()));
        }
        collection.add(item);
    }

    @Override
    public String toString() {
        return "Chunk " + getTrackingId();
    }
}
