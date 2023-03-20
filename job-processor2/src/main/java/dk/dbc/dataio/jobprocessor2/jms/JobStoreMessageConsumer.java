package dk.dbc.dataio.jobprocessor2.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.jobprocessor2.Config;
import dk.dbc.dataio.jobprocessor2.Metric;
import dk.dbc.dataio.jobprocessor2.ServiceHub;
import dk.dbc.dataio.jobprocessor2.exception.JobProcessorException;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jobprocessor2.service.HealthFlag;
import dk.dbc.dataio.jobprocessor2.service.HealthService;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.jms.artemis.AdminClient;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static dk.dbc.dataio.jobprocessor2.Config.ARTEMIS_ADMIN_PORT;
import static dk.dbc.dataio.jobprocessor2.Config.ARTEMIS_HOST;
import static dk.dbc.dataio.jobprocessor2.Config.ARTEMIS_PASSWORD;
import static dk.dbc.dataio.jobprocessor2.Config.ARTEMIS_USER;

/**
 * Handles Chunk messages received from the job-store
 */
public class JobStoreMessageConsumer implements MessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumer.class);
    private static final Duration SLOW_THRESHOLD_MS = Duration.ofMinutes(2);
    private static final Duration STALE_JMS_PROVIDER = Duration.ofMinutes(1);
    private static final int STALE_THRESHOLD = 20;
    private final AdminClient adminClient;

    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final ChunkProcessor chunkProcessor;
    private final HealthService healthService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<WatchKey, Instant> scriptStartTimes = new ConcurrentHashMap<>();

    public JobStoreMessageConsumer(ServiceHub serviceHub) {
        healthService = serviceHub.healthService;
        chunkProcessor = serviceHub.chunkProcessor;
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, r -> new Thread(r, "zombie-watch"));
        scheduledExecutorService.scheduleAtFixedRate(this::zombieWatch, 1, 1, TimeUnit.MINUTES);
        adminClient = ARTEMIS_ADMIN_PORT.asOptionalInteger()
                .map(port -> new AdminClient("http://" + ARTEMIS_HOST + ":" + port, ARTEMIS_USER.toString(), ARTEMIS_PASSWORD.toString()))
                .orElse(null);
        Metric.dataio_jobprocessor_chunk_duration_ms.gauge(this::getLongestRunningChunkDuration);
    }

    /**
     * Processes Chunk received in consumed message
     *
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     * @throws JobProcessorException   on general handling error
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = extractChunkFromConsumedMessage(consumedMessage);
        LOGGER.info("Received chunk {} for job {}", chunk.getChunkId(), chunk.getJobId());
        long flowId = consumedMessage.getHeaderValue(JmsConstants.FLOW_ID_PROPERTY_NAME, Long.class);
        long flowVersion = consumedMessage.getHeaderValue(JmsConstants.FLOW_VERSION_PROPERTY_NAME, Long.class);
        String additionalArgs = consumedMessage.getHeaderValue(JmsConstants.ADDITIONAL_ARGS, String.class);
        Flow flow = getFlow(chunk, flowId, flowVersion);
        sendResultToJobStore(processChunk(chunk, flow, additionalArgs));
    }

    @SuppressWarnings("unused")
    public void zombieWatch() {
        Instant now = Instant.now();
        scriptStartTimes.entrySet().stream()
                .filter(e -> Duration.between(e.getValue(), now).compareTo(HealthService.MAXIMUM_TIME_TO_PROCESS) > 0)
                .findFirst()
                .map(Map.Entry::getKey)
                .ifPresent(this::timeoutChunk);
        Duration sinceLastRun = Duration.ofMillis(getTimeSinceLastMessage());
        LOGGER.info("Time since last message: {}", sinceLastRun);
        if(adminClient != null && sinceLastRun.compareTo(STALE_JMS_PROVIDER) >= 0) {
            int count = getMessageCount();
            LOGGER.info("Messages on queue: {}, with filter: {}", count, Config.MESSAGE_FILTER.asOptionalString().orElse("<none>"));
            if(count > STALE_THRESHOLD) {
                LOGGER.error("MessageBean has gone stale, marking the server down");
                healthService.signal(HealthFlag.STALE);
            }
        }
    }

    private int getMessageCount() {
        String queue = Config.QUEUE.toString();
        try {
            return Config.MESSAGE_FILTER.asOptionalString().map(ms -> adminClient.countMessages(queue, queue, ms)).orElse(adminClient.getQueueAttribute(queue, queue, "Message count"));
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve message count for queue {}", queue, e);
            return 0;
        }
    }

    private long getLongestRunningChunkDuration() {
        long now = System.currentTimeMillis();
        return scriptStartTimes.values().stream().mapToLong(t -> now - t.toEpochMilli()).max().orElse(0);
    }

    private void timeoutChunk(WatchKey watchKey) {
        LOGGER.error("Processing of chunk id: {}, job: {} has exceeded its allowed time. Marking the server down!", watchKey.chunkId, watchKey.jobId);
        healthService.signal(HealthFlag.TIMEOUT);
    }

    private Chunk extractChunkFromConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            Chunk chunk = mapper.readValue(consumedMessage.getMessagePayload(), Chunk.class);
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            return chunk;
        } catch (JsonProcessingException e) {
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
                Metric.dataio_jobprocessor_slow_jobs.simpleTimer(tag).update(duration);
            }
        }
    }

    private Flow getFlow(Chunk chunk, long flowId, long flowVersion) throws JobProcessorException {
        return chunkProcessor.getCachedFlow(flowId, flowVersion).orElseGet(() -> flowFromJobStore(chunk));
    }

    private Flow flowFromJobStore(Chunk chunk) throws JobProcessorException {
        StopWatch stopWatch = new StopWatch();
        try {
            return jobStoreServiceConnector.getCachedFlow((int) chunk.getJobId());
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching flow for job %s", chunk.getJobId()), e);
        } finally {
            LOGGER.debug("Fetching flow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @SuppressWarnings("Duplicates")
    private void sendResultToJobStore(Chunk chunk) throws JobProcessorException {
        StopWatch stopWatch = new StopWatch();
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
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
