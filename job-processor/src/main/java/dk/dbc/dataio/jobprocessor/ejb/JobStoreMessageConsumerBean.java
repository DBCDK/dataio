package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.jms.artemis.AdminClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.Schedule;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Chunk messages received from the job-store
 */
@MessageDriven(name = "jobStoreListener", activationConfig = {
        // Please see the following url for an explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/processor"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "shard = '${ENV=PROCESSOR_SHARD}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "4"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "MaxSession", propertyValue = "4")
})
public class JobStoreMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumerBean.class);
    private static final Duration SLOW_THRESHOLD_MS = Duration.ofMinutes(2);
    private static final Duration STALE_JMS_PROVIDER = Duration.ofMinutes(5);
    private static final int STALE_THRESHOLD = 20;
    private AdminClient adminClient;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnector;
    @EJB
    ChunkProcessorBean chunkProcessor;
    @EJB
    CapacityBean capacityBean;

    @EJB
    private HealthBean healthBean;

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    @ConfigProperty(name = "ARTEMIS_MQ_HOST")
    private String artemisHost;

    @Inject
    @ConfigProperty(name = "ARTEMIS_ADMIN_PORT")
    private Integer artemisPort;

    @Inject
    @ConfigProperty(name = "ARTEMIS_USER")
    private String artemisUser;
    @Inject
    @ConfigProperty(name = "ARTEMIS_PASSWORD")
    private String artemisPassword;

    private final JSONBContext jsonbContext = new JSONBContext();

    private final Map<WatchKey, Instant> scriptStartTimes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        adminClient = artemisPort == null ? null : new AdminClient("http://" + artemisHost + ":" + artemisPort, artemisUser, artemisPassword);
        metricRegistry.gauge("dataio_jobprocessor_chunk_duration_ms", this::getLongestRunningChunkDuration);
    }

    /**
     * Processes Chunk received in consumed message
     *
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     * @throws JobProcessorException   on general handling error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, JobProcessorException {
        final Chunk chunk = extractChunkFromConsumedMessage(consumedMessage);
        LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
        final long flowId = consumedMessage.getHeaderValue(JmsConstants.FLOW_ID_PROPERTY_NAME, Long.class);
        final long flowVersion = consumedMessage.getHeaderValue(JmsConstants.FLOW_VERSION_PROPERTY_NAME, Long.class);
        final String additionalArgs = consumedMessage.getHeaderValue(JmsConstants.ADDITIONAL_ARGS, String.class);
        final Flow flow = getFlow(chunk, flowId, flowVersion);
        sendResultToJobStore(processChunk(chunk, flow, additionalArgs));
    }

    @SuppressWarnings("unused")
    @Schedule(minute = "*", hour = "*")
    public void zombieWatch() {
        Instant now = Instant.now();
        scriptStartTimes.entrySet().stream()
                .filter(e -> Duration.between(e.getValue(), now).compareTo(CapacityBean.MAXIMUM_TIME_TO_PROCESS) > 0)
                .findFirst()
                .map(Map.Entry::getKey)
                .ifPresent(this::timeoutChunk);
        Duration sinceLastRun = Duration.ofMillis(getTimeSinceLastMessage());
        LOGGER.info("Time since last message: {}", sinceLastRun);
        if(adminClient != null && sinceLastRun.compareTo(STALE_JMS_PROVIDER) >= 0) {
            String messageSelector = "shard = '" + System.getenv("PROCESSOR_SHARD") + "'";
            int count = adminClient.countMessages("jmsDataioProcessor", "jmsDataioProcessor", messageSelector);

            LOGGER.info("Messages on queue: {}, with filter: {}", count, messageSelector);
            if(count > STALE_THRESHOLD) {
                LOGGER.error("MessageBean has gone stale, marking the server down");
                healthBean.signalTerminallyIll(new IllegalStateException("Message bean seems to be stuck"));
            }
        }
    }

    private long getLongestRunningChunkDuration() {
        long now = System.currentTimeMillis();
        return scriptStartTimes.values().stream().mapToLong(t -> now - t.toEpochMilli()).max().orElse(0);
    }

    private void timeoutChunk(WatchKey watchKey) {
        LOGGER.error("Processing of chunk id: {}, job: {} has exceeded its allowed time. Marking the server down!", watchKey.chunkId, watchKey.jobId);
        capacityBean.signalTimeout();
    }

    private Chunk extractChunkFromConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            return chunk;
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getHeaderValue(JmsConstants.CHUNK_PAYLOAD_TYPE, String.class)), e);
        }
    }

    private Chunk processChunk(Chunk chunk, Flow flow, String additionalArgs) {
        WatchKey key = new WatchKey(chunk, flow);
        Instant start = Instant.now();
        scriptStartTimes.put(key, start);
        try {
            return chunkProcessor.process(chunk, flow, additionalArgs);
        } finally {
            scriptStartTimes.remove(key);
            Duration duration = Duration.between(start, Instant.now());
            if(duration.compareTo(SLOW_THRESHOLD_MS) > 0) {
                Tag tag = new Tag("flow", key.flow);
                metricRegistry.simpleTimer("dataio_jobprocessor_slow_jobs", tag).update(duration);
            }
        }
    }

    private Flow getFlow(Chunk chunk, long flowId, long flowVersion) throws JobProcessorException {
        return chunkProcessor.getCachedFlow(flowId, flowVersion).orElseGet(() -> flowFromJobStore(chunk));
    }

    private Flow flowFromJobStore(Chunk chunk) throws JobProcessorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            return jobStoreServiceConnector.getConnector().getCachedFlow((int) chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching flow for job %s", chunk.getJobId()), e);
        } finally {
            LOGGER.debug("Fetching flow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @SuppressWarnings("Duplicates")
    private void sendResultToJobStore(Chunk chunk) throws JobProcessorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            jobStoreServiceConnector.getConnector().addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new JobProcessorException("Error while sending result to job-store", e);
        } finally {
            LOGGER.debug("Sending result took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public static class WatchKey {
        public final long chunkId;
        public final long jobId;
        public final String flow;
        public final long threadId;

        public WatchKey(Chunk chunk, Flow flow) {
            chunkId = chunk.getChunkId();
            jobId = chunk.getJobId();
            this.flow = String.join(", ", "id=" + flow.getId(), "version=" + flow.getVersion(), "name=" + flow.getContent().getName());
            threadId = Thread.currentThread().getId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WatchKey watchKey = (WatchKey) o;
            return chunkId == watchKey.chunkId && jobId == watchKey.jobId && threadId == watchKey.threadId && Objects.equals(flow, watchKey.flow);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkId, jobId, flow, threadId);
        }
    }
}
