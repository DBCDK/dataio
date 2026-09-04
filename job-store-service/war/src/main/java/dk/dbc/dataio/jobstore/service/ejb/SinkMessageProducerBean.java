package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.types.jms.MessageIdentifiers;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.TextMessage;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * This Enterprise Java Bean (EJB) functions as JMS message producer for
 * communication going to the sinks
 */
@LocalBean
@Stateless
public class SinkMessageProducerBean extends AbstractMessageProducer implements MessageIdentifiers {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinkMessageProducerBean.class);
    /**
     * JMS defined property the broker serializes message delivery on, one consumer at a
     * time per group. Not a dataIO header, so it has no {@link JMSHeader} constant.
     */
    private static final String JMSX_GROUP_ID = "JMSXGroupID";
    private static final String RECORD_KEY_DELIMITER = ":";
    private final RetryPolicy<?> retryPolicy;
    JSONBContext jsonbContext = new JSONBContext();
    @Inject
    @ConfigProperty(name = "ARTEMIS_MQ_HOST")
    private String artemisHost;

    public SinkMessageProducerBean() {
        this(new RetryPolicy<>().handle(JMSRuntimeException.class).withDelay(Duration.ofSeconds(30)).withMaxRetries(10)
                .onFailedAttempt(attempt -> LOGGER.warn("Unable to send message to sink", attempt.getLastFailure())));
    }

    public SinkMessageProducerBean(RetryPolicy<?> retryPolicy) {
        super(JobEntity::getSinkQueue);
        this.retryPolicy = retryPolicy;
    }

    @PostConstruct
    public void init() {
        connectionFactory = new ActiveMQXAConnectionFactory("tcp://" + artemisHost + ":61616");
    }

    /**
     * Sends the processing outcome of each of the given items as an individual JMS message
     * with JSON payload to the sink queue destination
     * <p>
     * The items must be ordered by ascending item ID. Two items of the same chunk can share
     * a {@link RecordInfo#getCorrelationKey() correlationKey}, either as two versions of the
     * same record or as two records of the same hierarchy, and the broker serializes a group
     * in the order its messages were sent. Sending out of order would let a lower
     * {@code (jobId, chunkId, itemId)} version reach the sink after a higher one.
     * <p>
     * All messages of a chunk are produced through a single {@link JMSContext}, so that the
     * enlisted XA session makes either all of them or none of them visible to the sink.
     *
     * @param items    items of a single processed chunk, ordered by ascending item ID,
     *                 not empty and each carrying a processing outcome
     * @param job      job to which the items belong
     * @param priority message priority
     * @throws JobStoreException when the items cannot be delivered as they are, or when
     *                           unable to send an item to destination
     */
    public void send(List<ItemEntity> items, JobEntity job, int priority) throws JobStoreException {
        FlowStoreReferences flowStoreReferences = job.getFlowStoreReferences();
        long agencyId = job.getSpecification().getSubmitterId();
        verifyDeliverable(items);
        ItemEntity.Key firstItemKey = items.get(0).getKey();
        Failsafe.with(retryPolicy).run(() -> {
            try (JMSContext context = connectionFactory.createContext()) {
                LOGGER.info("Sending {} items of chunk {}/{} to queue {}",
                        items.size(), firstItemKey.getJobId(), firstItemKey.getChunkId(), job.getSinkQueue());
                for (ItemEntity item : items) {
                    TextMessage message = createItemMessage(context, item, flowStoreReferences, agencyId);
                    send(context, message, job, priority);
                }
            } catch (JSONBException | JMSException e) {
                String errorMessage = String.format(
                        "Exception caught while sending items of processed chunk %d in job %d",
                        firstItemKey.getChunkId(),
                        firstItemKey.getJobId());
                throw new JobStoreException(errorMessage, e);
            }
        });
    }

    /**
     * Rejects the whole chunk before any of its messages are produced, rather than letting
     * a single unusable item become a body of JSON {@code null} that no sink can make sense
     * of and no sink reports an error for. A {@link JobStoreException} puts the chunk back
     * in {@code SCHEDULED_FOR_DELIVERY} for a later attempt, which is what the caller does
     * with any other send failure.
     */
    private void verifyDeliverable(List<ItemEntity> items) throws JobStoreException {
        if (items.isEmpty()) {
            throw new JobStoreException("No items to deliver");
        }
        for (ItemEntity item : items) {
            if (item.getProcessingOutcome() == null) {
                throw new JobStoreException(String.format(
                        "Item %d/%d/%d has no processing outcome to deliver",
                        item.getKey().getJobId(), item.getKey().getChunkId(), item.getKey().getId()));
            }
        }
    }

    /**
     * Creates new TextMessage with the given item's processing outcome as JSON payload
     * <p>
     * Every routing and validation header the chunk message carries is carried here as well,
     * because splitting a chunk message into N item messages does not change what the sink
     * framework reads off a message before any per-sink code runs, and the failure modes are
     * silent rather than loud. A missing {@code payload} header has the message discarded
     * with a warning and no rollback, so the item is lost with no retry, and a missing
     * {@code sinkId}, {@code sinkVersion}, {@code flowBinderId} or {@code flowBinderVersion}
     * is an NPE in the sinks that unbox them during config refresh.
     *
     * @param context             active JMS context
     * @param item                item whose processing outcome is added as JSON string payload
     * @param flowStoreReferences flow-store references for the job to which the given item belongs
     * @param agencyId            submitter number of the job, qualifying the record key
     * @return TextMessage instance
     * @throws JSONBException when unable to marshall the processing outcome to JSON
     * @throws JMSException   when unable to create JMS message
     */
    public TextMessage createItemMessage(JMSContext context, ItemEntity item, FlowStoreReferences flowStoreReferences,
                                         long agencyId) throws JMSException, JSONBException {
        FlowStoreReference sinkReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        ItemEntity.Key key = item.getKey();

        TextMessage message = context.createTextMessage(jsonbContext.marshall(item.getProcessingOutcome()));
        JMSHeader.payload.addHeader(message, JMSHeader.ITEM_PAYLOAD_TYPE);
        JMSHeader.sinkId.addHeader(message, sinkReference.getId());
        JMSHeader.sinkVersion.addHeader(message, sinkReference.getVersion());
        addItemIdentifiers(message, key.getJobId(), key.getChunkId(), key.getId());
        // if the execution is towards the diff sink during an acceptance test run
        if (flowBinderReference != null) {
            JMSHeader.flowBinderId.addHeader(message, flowBinderReference.getId());
            JMSHeader.flowBinderVersion.addHeader(message, flowBinderReference.getVersion());
        }
        addRecordIdentity(message, item.getRecordInfo(), agencyId);
        return message;
    }

    /**
     * Adds the two headers derived from the item's record, when it has one
     * <p>
     * {@link JMSHeader#recordKey} is composed here, in this one place, as
     * {@code <agencyId>:RecordInfo.getId()} and is opaque to sinks. It is composed rather
     * than re-derived by each sink from the delivered content because
     * {@link RecordInfo}'s constructor whitespace normalizes the ID, so a sink reading the
     * raw record bytes would compose a key that never matches the one job-store compares
     * against, and stale delivery detection would silently stop working. The agency
     * qualification is what keeps the key as unique as {@code RecordInfo.getId()} actually
     * is, since a record ID is only meaningful within the agency that assigned it.
     * {@code getSubmitterId()} returns a long whose decimal form can never contain a colon,
     * so the composition is injective and the first colon is always the delimiter.
     * <p>
     * {@code JMSXGroupID} is the correlation key, which is what the broker serializes
     * deliveries on. It is deliberately not agency qualified: over grouping two agencies'
     * records that share a literal ID only costs a little latency, whereas over grouping a
     * watermark key would produce a wrong SUPERSEDED verdict for an unrelated record.
     * <p>
     * An item without a record, such as the job termination item, gets neither header. It
     * is then distributed freely by the broker and skipped by the watermark check, which is
     * correct: it is a per-job barrier, not a bibliographic record.
     */
    private void addRecordIdentity(TextMessage message, RecordInfo recordInfo, long agencyId) throws JMSException {
        if (recordInfo == null) {
            return;
        }
        if (recordInfo.getId() != null) {
            JMSHeader.recordKey.addHeader(message, agencyId + RECORD_KEY_DELIMITER + recordInfo.getId());
        }
        if (recordInfo.getCorrelationKey() != null) {
            message.setStringProperty(JMSX_GROUP_ID, recordInfo.getCorrelationKey());
        }
    }
}
