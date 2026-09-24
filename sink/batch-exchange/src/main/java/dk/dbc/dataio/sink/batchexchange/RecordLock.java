package dk.dbc.dataio.sink.batchexchange;

import jakarta.persistence.EntityManager;

/**
 * Serializes work on one record across every sink instance, for the length of a transaction.
 * <p>
 * Two things decide what happens to a record: a message consumer handling a new version of it,
 * and {@link BatchFinalizer} releasing it when its batch completes. The broker's
 * {@code JMSXGroupID} grouping orders the consumers against each other, and covers the
 * finalizer not at all, since the finalizer is a timer rather than a message consumer. Both
 * read the record's staged and held rows and then act on what they read, so without this the
 * finalizer can stage a held version while a consumer is midway through replacing it, and the
 * consumer then reports as superseded a version that is on its way to the target.
 * <p>
 * A transaction-scoped advisory lock is used rather than row locks because there is often no
 * row to lock: the decision being serialized includes the case where the record has nothing
 * staged and nothing held. The lock is released when the transaction commits or rolls back,
 * with no explicit release to lose.
 */
public class RecordLock {
    /* hashtextextended gives the 64 bits the lock takes. Two record keys hashing alike wait for
       each other needlessly, which costs throughput and nothing else. */
    private static final String ACQUIRE = "SELECT pg_advisory_xact_lock(hashtextextended(?1, 0))";

    private RecordLock() {
    }

    /**
     * Takes the lock on the given item's record, waiting for whoever holds it
     * <p>
     * An item with no record key has no record to serialize on, and no version of it can be
     * held behind anything, so nothing is taken for one.
     *
     * @param entityManager entity manager of the transaction holding the lock
     * @param batchName     identity of the item whose record is being worked on
     */
    public static void acquire(EntityManager entityManager, BatchName batchName) {
        if (batchName.getRecordKey() == null) {
            return;
        }
        acquire(entityManager, batchName.getSinkId(), batchName.getRecordKey());
    }

    /**
     * Takes the lock on the given record, waiting for whoever holds it
     *
     * @param entityManager entity manager of the transaction holding the lock
     * @param sinkId        sink the record belongs to
     * @param recordKey     the record being worked on
     */
    public static void acquire(EntityManager entityManager, long sinkId, String recordKey) {
        entityManager.createNativeQuery(ACQUIRE)
                .setParameter(1, sinkId + ":" + recordKey)
                .getSingleResult();
    }
}
