package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.MetaData;
import jakarta.persistence.EntityManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes an item's records into the batch exchange as a batch of pending entries, together with
 * the bookkeeping row naming the item the batch was created for.
 * <p>
 * Two paths stage an item: {@link BatchExchangeMessageConsumer} when the item arrives and no
 * other version of its record is in flight, and {@link BatchFinalizer} when it drains a version
 * that was held back. Both produce the same batch, so both go through here.
 */
public class BatchStager {
    private BatchStager() {
    }

    /**
     * Reads an item's addi records into pending batch entries
     * <p>
     * Every entry but the last is marked as continued, which is how the consumer system is told
     * that the entries belong to one unit of work.
     *
     * @param data the item's addi records
     * @return entries to stage, empty when the item carries no records
     * @throws IOException if the data is not readable as addi records
     */
    public static List<BatchEntry> entriesFrom(byte[] data) throws IOException {
        List<BatchEntry> entries = new ArrayList<>();
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(data));
        while (addiReader.hasNext()) {
            BatchEntry entry = pendingEntry(addiReader.next());
            if (addiReader.hasNext()) {
                entry.withIsContinued(true);
            }
            entries.add(entry);
        }
        return entries;
    }

    /**
     * Stages the entries as one batch and records the item they belong to
     *
     * @param entityManager  entity manager of the transaction the batch is created in
     * @param batchName      identity of the item being staged
     * @param entries        the item's entries, which must not be empty: a batch with no entries
     *                       never completes, so the finalizer would never report the item and
     *                       the job would never finish
     * @param priority       priority the consumer system claims the entries by
     * @param trackingId     tracking id stamped on every entry, so that the consumer system's
     *                       answer can be traced back to the item it belongs to
     * @param collapsedCount number of versions of this record skipped to arrive at this one
     * @param collapsedFrom  oldest version skipped to arrive at this one, or null when none was
     * @return id of the batch created
     */
    public static int stage(EntityManager entityManager, BatchName batchName, List<BatchEntry> entries,
                            int priority, String trackingId, int collapsedCount, String collapsedFrom) {
        Batch batch = new Batch().withName(batchName.toString());
        entityManager.persist(batch);
        entityManager.flush();
        entityManager.refresh(batch);
        for (BatchEntry entry : entries) {
            entityManager.persist(entry
                    .withBatch(batch.getId())
                    .withTrackingId(trackingId)
                    .withPriority(priority));
        }
        entityManager.persist(StagedItem.of(batch.getId(), batchName)
                .withCollapsed(collapsedCount, collapsedFrom));
        return batch.getId();
    }

    /**
     * Stages an item that was held back, as the batch it would have become had nothing been in
     * flight for its record
     * <p>
     * What it collapsed on its way to being staged travels with it, so that an outcome reported
     * for it later can say which versions were skipped to arrive at it.
     *
     * @param entityManager entity manager of the transaction the batch is created in
     * @param heldItem  the held item, whose row is removed as it is staged
     * @return id of the batch created
     * @throws IllegalStateException if the held records are no longer readable, which cannot
     *                               happen for records that were read once before being held
     */
    public static int stageHeldItem(EntityManager entityManager, HeldItem heldItem) {
        BatchName batchName = heldItem.batchName();
        List<BatchEntry> entries;
        try {
            entries = entriesFrom(heldItem.getPayload());
        } catch (IOException e) {
            throw new IllegalStateException("Unreadable records held for item " + batchName, e);
        }
        String trackingId = heldItem.getTrackingId();
        if (trackingId == null) {
            trackingId = batchName.asTrackingId();
        }
        int batch = stage(entityManager, batchName, entries, heldItem.getPriority(), trackingId,
                heldItem.getCollapsedCount(), heldItem.getCollapsedFrom());
        entityManager.remove(heldItem);
        return batch;
    }

    private static BatchEntry pendingEntry(AddiRecord addiRecord) throws IOException {
        return new BatchEntry()
                .withContent(addiRecord.getContentData())
                .withMetadata(MetaData.fromXml(addiRecord.getMetaData()).getInfoJson());
    }
}
