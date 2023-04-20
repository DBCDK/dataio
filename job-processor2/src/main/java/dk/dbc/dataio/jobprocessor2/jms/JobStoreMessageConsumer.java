package dk.dbc.dataio.jobprocessor2.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobprocessor2.Metric;
import dk.dbc.dataio.jobprocessor2.ProcessorConfig;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jobprocessor2.service.HealthFlag;
import dk.dbc.dataio.jse.artemis.common.JobProcessorException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Chunk messages received from the job-store
 */
public class JobStoreMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreMessageConsumer.class);
    private static final Duration SLOW_THRESHOLD_MS = Duration.ofMinutes(2);

    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final ChunkProcessor chunkProcessor;
    private final HealthService healthService;
    private static final Map<WatchKey, Instant> scriptStartTimes = new ConcurrentHashMap<>();
    private static final String QUEUE = ProcessorConfig.QUEUE.toString();
    private static final String FILTER = ProcessorConfig.MESSAGE_FILTER.asOptionalString().orElse(null);

    public JobStoreMessageConsumer(ServiceHub serviceHub) {
        super(serviceHub);
        healthService = serviceHub.healthService;
        chunkProcessor = new ChunkProcessor(healthService);
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        Metric.dataio_jobprocessor_chunk_duration_ms.gauge(this::getLongestRunningChunkDuration);
        initMetrics(PrometheusMetricRegistry.create());
        zombieWatch.addCheck(this::scriptRuntimeCheck);
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

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getFilter() {
        return FILTER;
    }

    @SuppressWarnings("unused")
    public void scriptRuntimeCheck() {
        Instant now = Instant.now();
        scriptStartTimes.entrySet().stream()
                .filter(e -> Duration.between(e.getValue(), now).compareTo(HealthService.MAXIMUM_TIME_TO_PROCESS) > 0)
                .findFirst()
                .map(Map.Entry::getKey)
                .ifPresent(this::timeoutChunk);

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
            Chunk chunk = MAPPER.readValue(consumedMessage.getMessagePayload(), Chunk.class);
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
