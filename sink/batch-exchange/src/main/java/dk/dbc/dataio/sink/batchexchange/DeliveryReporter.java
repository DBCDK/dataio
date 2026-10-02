package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;

import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.destination;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.status;
import static dk.dbc.dataio.jse.artemis.common.Metric.dataio_item_delivery_count;

/**
 * Reports an item's delivery to job-store on behalf of this sink.
 * <p>
 * Two places in this sink report an item themselves rather than returning a result for the sink
 * framework to report: {@link BatchFinalizer}, once the consumer system has answered for a
 * staged item, and {@link BatchExchangeMessageConsumer}, for a version it collapses while
 * handling a different item. Both name the watermark row the outcome concerns and count the
 * delivery under the same destination, so that what they report and what the framework reports
 * form one series.
 */
public class DeliveryReporter {
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final String fqn;

    public DeliveryReporter(JobStoreServiceConnector jobStoreServiceConnector, String fqn) {
        this.jobStoreServiceConnector = jobStoreServiceConnector;
        this.fqn = fqn;
    }

    /**
     * Reports the item's delivery, naming the watermark row it may advance
     * <p>
     * A record key the item arrived without stays null, which is what tells job-store the item
     * has no watermark row rather than that this outcome must not advance one.
     *
     * @param batchName identity of the item being reported
     * @param verdict   how job-store counts the item and whether the record's watermark moves
     * @param outcome   the item's delivering outcome, stored verbatim
     * @throws RuntimeException if job-store could not be reached
     */
    public void report(BatchName batchName, ItemDeliveryResult.Status verdict, ChunkItem outcome) {
        ItemDeliveryResult result = ItemDeliveryResult.of(verdict, outcome)
                .withWatermarkKey(batchName.getSinkId(), batchName.getRecordKey());
        try {
            jobStoreServiceConnector.addItemDelivered(result,
                    batchName.getJobId(), (int) batchName.getChunkId(), batchName.getItemId());
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Error in communication with job-store for item %s", batchName), e);
        }
        dataio_item_delivery_count.counter(destination.is(fqn), status.is(verdict.name())).inc();
    }
}
