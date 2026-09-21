package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Stages one item's records in the batch exchange for a consumer system to collect.
 * <p>
 * Nothing is delivered to a target here, so no verdict is available when {@code deliverItem}
 * returns. The item's records become a batch of pending entries, the consumer system answers
 * for them in its own time, and {@link BatchFinalizer} reports the outcome, which is what
 * {@link #defersDeliveryResult()} declares. An item with nothing to stage is decided on the
 * spot instead.
 * <p>
 * A record has at most one version staged at a time. The consumer system applies claimed
 * entries across a thread pool without serializing by record, so two versions staged at once
 * are applied in an undefined order, and the target can end up holding the older one under a
 * watermark naming the newer. A version that arrives while another is in flight is therefore
 * held in {@code held_item} until the staged one completes.
 */
public class BatchExchangeMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchExchangeMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final EntityManagerFactory entityManagerFactory;
    private final DeliveryReporter deliveryReporter;

    public BatchExchangeMessageConsumer(ServiceHub serviceHub, EntityManagerFactory entityManagerFactory) {
        super(serviceHub);
        this.entityManagerFactory = entityManagerFactory;
        this.deliveryReporter = new DeliveryReporter(jobStoreServiceConnector, fqn());
    }

    /**
     * Gives the destination this sink's deliveries are counted under, for
     * {@link BatchFinalizer}, which reports the deliveries this class defers and is not a
     * message consumer with a destination of its own
     * <p>
     * Built the way {@code MessageConsumer.getFQN} builds it, so that the deliveries reported
     * there and the ones reported here count towards one series rather than two.
     *
     * @return fully qualified name of the queue this sink consumes
     */
    static String fqn() {
        return ADDRESS + "::" + QUEUE;
    }

    /**
     * Reports the outcome of a staged item from {@link BatchFinalizer} rather than here
     *
     * @return true, this sink's target answers long after an item has been accepted
     */
    @Override
    protected boolean defersDeliveryResult() {
        return true;
    }

    /**
     * Stages a successfully processed item's records for the consumer system, and passes any
     * other item through as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered,
     * so that it counts towards the job's ignored items and advances no delivery watermark for
     * a record nothing was sent for. It reaches the consumer system no more than it reaches a
     * target, so it is answered here instead of becoming an entry that completes immediately.
     *
     * @return the outcome of an item there was nothing to stage for, or null for an item whose
     * outcome the consumer system still owes
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        return switch (item.getStatus()) {
            case FAILURE -> ignored(item, "Failed by processor");
            case IGNORE -> ignored(item, "Ignored by processor");
            case SUCCESS -> stage(message, item);
        };
    }

    /**
     * Discards the job's staged and held work
     * <p>
     * A version of another job's record held behind one of the discarded batches is staged
     * rather than discarded with it, since nothing would be left to release it otherwise and
     * its own job would never finish.
     */
    @Override
    public void abortJob(int jobId) {
        int batches;
        int held;
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            batches = deleteBatches(jobId, entityManager);
            held = deleteHeld(jobId, entityManager);
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
        int released = stageReleasedItems();
        LOGGER.warn("Aborted job {}, deleted {} batches and {} held items, staged {} held items"
                + " of other jobs", jobId, batches, held, released);
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    /**
     * Stages the item's records as a batch of pending entries, holds them back while another
     * version of the same record is in flight, or skips them for a newer version
     *
     * @return null once the item is staged or held and its outcome is still owed, or an outcome
     * for an item that will not be staged at all
     */
    private ItemDeliveryResult stage(ConsumedMessage message, ChunkItem item) {
        BatchName batchName = BatchName.fromMessage(message);
        List<BatchEntry> entries;
        try {
            entries = BatchStager.entriesFrom(item.getData());
        } catch (RuntimeException | IOException e) {
            return failed(item, e);
        }
        if (entries.isEmpty()) {
            /* Staging nothing would create a batch that never completes, so the finalizer would
               never report the item and the job would never finish. */
            return ignored(item, "No records to deliver");
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            /* Taken before the first read, so that nothing is decided on a view of this record
               that another instance, or this instance's finalizer, is in the middle of changing.
               Taking it first also means no row of this record is held while waiting for it,
               which is what keeps the wait free of cycles. */
            RecordLock.acquire(entityManager, batchName);
            StagedItem staged = stagedItemFor(batchName, entityManager);
            if (staged != null) {
                BatchName stagedName = staged.batchName();
                if (stagedName.isSameItemAs(batchName)) {
                    LOGGER.info("Item {} is already staged, leaving its result to the finalizer", batchName);
                    return null;
                }
                if (batchName.isOlderThan(stagedName)) {
                    LOGGER.info("Skipping item {}, newer version {} is already staged", batchName, stagedName);
                    return superseded(item, String.format(
                            "version %s of this record is already staged for delivery",
                            stagedName.asItemReference()));
                }
                ItemDeliveryResult result = hold(message, item, batchName, entityManager);
                transaction.commit();
                return result;
            }
            ItemDeliveryResult result = stageNow(message, item, batchName, entries, entityManager);
            transaction.commit();
            return result;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    /**
     * Holds the item back until the staged version of its record completes
     * <p>
     * A version already held is replaced rather than queued behind, and is reported as
     * superseded as it is displaced. Only the newest version of a record ever waits, which is
     * the rule the delivery watermark applies to versions that reached the target.
     *
     * @return null once the item is held, or an outcome for an item a newer held version
     * already displaced
     */
    private ItemDeliveryResult hold(ConsumedMessage message, ChunkItem item, BatchName batchName,
                                    EntityManager entityManager) {
        HeldItem incoming = HeldItem.of(message, item, batchName);
        HeldItem held = heldItemFor(batchName, entityManager);
        if (held != null) {
            BatchName heldName = held.batchName();
            if (heldName.isSameItemAs(batchName)) {
                LOGGER.info("Item {} is already held, leaving its result until it is staged", batchName);
                return null;
            }
            if (batchName.isOlderThan(heldName)) {
                LOGGER.info("Skipping item {}, newer version {} is already held", batchName, heldName);
                return superseded(item, String.format(
                        "version %s of this record is already held for delivery",
                        heldName.asItemReference()));
            }
            incoming.displacing(held);
            entityManager.remove(held);
            /* The unique key over sink and record is held by the row being removed, so the
               delete has to reach the database before the insert taking its place. */
            entityManager.flush();
            reportDisplaced(held, batchName);
        }
        entityManager.persist(incoming);
        LOGGER.info("Holding item {} until the staged version of its record completes", batchName);
        return null;
    }

    /**
     * Stages the item, taking over anything held for its record
     * <p>
     * Nothing is staged for the record at this point, so a held version has nothing left to
     * wait for and is folded into this one rather than left for a drain that will never run.
     *
     * @return null once the item is staged, or an outcome for an item a newer held version
     * already displaced
     */
    private ItemDeliveryResult stageNow(ConsumedMessage message, ChunkItem item, BatchName batchName,
                                        List<BatchEntry> entries, EntityManager entityManager) {
        int collapsedCount = 0;
        String collapsedFrom = null;
        HeldItem held = heldItemFor(batchName, entityManager);
        if (held != null) {
            BatchName heldName = held.batchName();
            if (batchName.isOlderThan(heldName)) {
                BatchStager.stageHeldItem(entityManager, held);
                LOGGER.info("Skipping item {}, staged newer held version {} instead", batchName, heldName);
                return superseded(item, String.format(
                        "version %s of this record was staged for delivery in its place",
                        heldName.asItemReference()));
            }
            collapsedCount = held.getCollapsedCount();
            collapsedFrom = held.getCollapsedFrom();
            if (!heldName.isSameItemAs(batchName)) {
                collapsedCount++;
                if (collapsedFrom == null) {
                    collapsedFrom = heldName.asItemReference();
                }
                reportDisplaced(held, batchName);
            }
            entityManager.remove(held);
            entityManager.flush();
        }
        int batch = BatchStager.stage(entityManager, batchName, entries,
                message.getPriority().getValue(), trackingId(item, batchName), collapsedCount, collapsedFrom);
        LOGGER.info("Staged item {} as batch {} with {} entries", batchName, batch, entries.size());
        return null;
    }

    /**
     * Gives the item this sink holds staged for the given item's record, if any
     *
     * @return the staged item, or null when the record has none in flight
     */
    private StagedItem stagedItemFor(BatchName batchName, EntityManager entityManager) {
        if (batchName.getRecordKey() == null) {
            /* An item with no record key has no record identity, so nothing can be in flight
               for it and nothing can supersede it. */
            return null;
        }
        TypedQuery<StagedItem> query = entityManager.createQuery(
                "select s from StagedItem s where s.sinkId = :sinkId and s.recordKey = :recordKey",
                StagedItem.class);
        query.setParameter("sinkId", batchName.getSinkId());
        query.setParameter("recordKey", batchName.getRecordKey());
        return query.getResultStream().findFirst().orElse(null);
    }

    /**
     * Gives the item this sink holds back for the given item's record, if any
     *
     * @return the held item, or null when the record has none
     */
    private HeldItem heldItemFor(BatchName batchName, EntityManager entityManager) {
        if (batchName.getRecordKey() == null) {
            return null;
        }
        TypedQuery<HeldItem> query = entityManager.createQuery(
                "select d from HeldItem d where d.sinkId = :sinkId and d.recordKey = :recordKey",
                HeldItem.class);
        query.setParameter("sinkId", batchName.getSinkId());
        query.setParameter("recordKey", batchName.getRecordKey());
        return query.getResultStream().findFirst().orElse(null);
    }

    /**
     * Stages every held item whose record has nothing staged for it any more
     * <p>
     * One transaction per record rather than one for all of them, so that only one record is
     * locked at a time. Holding several would let this wait on a record a consumer holds while
     * that consumer waits on a record held here.
     *
     * @return number of items staged
     */
    private int stageReleasedItems() {
        int staged = 0;
        for (HeldItem released : releasedItems()) {
            if (stageReleasedItem(released)) {
                staged++;
            }
        }
        return staged;
    }

    private List<HeldItem> releasedItems() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager.createQuery(
                            "select d from HeldItem d where not exists"
                                    + " (select s from StagedItem s where s.sinkId = d.sinkId"
                                    + " and s.recordKey = d.recordKey)", HeldItem.class)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    /**
     * Stages one released item, if it is still both held and released once its record is locked
     *
     * @return true if the item was staged
     */
    private boolean stageReleasedItem(HeldItem released) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            RecordLock.acquire(entityManager, released.getSinkId(), released.getRecordKey());
            /* Read under the lock rather than trusting what the listing saw, since a consumer
               may have replaced the held version or staged one of its own in between. */
            HeldItem held = entityManager.find(HeldItem.class, released.getId());
            if (held == null || stagedItemFor(held.batchName(), entityManager) != null) {
                return false;
            }
            int batch = BatchStager.stageHeldItem(entityManager, held);
            transaction.commit();
            LOGGER.info("Staged released item {} as batch {}", held.batchName(), batch);
            return true;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    private int deleteBatches(int jobId, EntityManager entityManager) {
        List<Integer> batches = entityManager.createQuery(
                        "select s.batch from StagedItem s where s.jobId = :jobId", Integer.class)
                .setParameter("jobId", jobId)
                .getResultList();
        if (batches.isEmpty()) {
            return 0;
        }
        /* Removing the batch cascades to both its entries and its bookkeeping row. */
        return entityManager.createQuery("delete from Batch b where b.id in :batches")
                .setParameter("batches", batches)
                .executeUpdate();
    }

    private int deleteHeld(int jobId, EntityManager entityManager) {
        return entityManager.createQuery("delete from HeldItem d where d.jobId = :jobId")
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    /**
     * Reports a held version that a newer one takes the place of
     * <p>
     * Reported here rather than by {@link BatchFinalizer}, which only ever sees versions that
     * were staged, and a displaced version never is.
     */
    private void reportDisplaced(HeldItem displaced, BatchName replacement) {
        BatchName displacedName = displaced.batchName();
        String message = String.format(
                "Item was skipped, version %s of this record replaced it before delivery",
                replacement.asItemReference());
        ChunkItem outcome = new ChunkItem()
                .withId(displacedName.getItemId())
                .withTrackingId(displaced.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8)
                .withStatus(ChunkItem.Status.IGNORE)
                .withDiagnostics(new Diagnostic(Diagnostic.Level.WARNING, message))
                .withData(message);
        deliveryReporter.report(displacedName, ItemDeliveryResult.Status.SUPERSEDED, outcome);
    }

    /**
     * Gives the tracking id to stamp on the item's entries, so that the consumer system's
     * answer can be traced back to the item it belongs to
     */
    private String trackingId(ChunkItem chunkItem, BatchName batchName) {
        String trackingId = chunkItem.getTrackingId();
        if (trackingId == null) {
            return batchName.asTrackingId();
        }
        return trackingId;
    }

    private ItemDeliveryResult ignored(ChunkItem item, String reason) {
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                outcome(item)
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.WARNING, reason))
                        .withData(reason));
    }

    private ItemDeliveryResult failed(ChunkItem item, Exception cause) {
        String message = cause.getMessage();
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                outcome(item)
                        .withStatus(ChunkItem.Status.FAILURE)
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, message, cause))
                        .withData(StringUtil.getStackTraceString(cause)));
    }

    /**
     * Gives the outcome of an item another version of the same record took the place of
     * <p>
     * Returned by this sink rather than by the sink framework, which can only see versions
     * whose delivery has been reported. For this sink a delivery is reported once the consumer
     * system has answered, so a version in flight or held back is invisible there and visible
     * here.
     */
    private ItemDeliveryResult superseded(ChunkItem item, String reason) {
        String message = "Item was skipped, " + reason;
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.SUPERSEDED,
                outcome(item)
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.WARNING, message))
                        .withData(message));
    }

    /**
     * Creates the delivering outcome item of an item decided here, without the status and data
     * naming what became of it
     */
    private ChunkItem outcome(ChunkItem item) {
        return new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
    }
}
