package dk.dbc.dataio.jobstore.types;

import java.util.Comparator;

/**
 * The newest version of a record delivered to a given sink so far, as a
 * {@code (jobId, chunkId, itemId)} version tuple (see docs/chunk-scheduling-redesign.md).
 * <p>
 * The natural ordering compares the three components in that order, matching the row
 * comparison the {@code sink_record_delivery_watermark} upsert makes in SQL. The two must
 * agree: the sink decides whether to deliver by comparing here, and job-store decides
 * whether to advance the watermark by comparing there.
 */
public record Watermark(int jobId, int chunkId, short itemId) implements Comparable<Watermark> {
    private static final Comparator<Watermark> COMPARATOR = Comparator
            .comparingInt(Watermark::jobId)
            .thenComparingInt(Watermark::chunkId)
            .thenComparingInt(Watermark::itemId);

    @Override
    public int compareTo(Watermark other) {
        return COMPARATOR.compare(this, other);
    }
}
