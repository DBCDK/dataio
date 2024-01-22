package dk.dbc.dataio.jobprocessor2.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jobprocessor2.Metric;
import dk.dbc.dataio.jobprocessor2.ProcessorConfig;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jobprocessor2.service.HealthFlag;
import dk.dbc.dataio.jse.artemis.common.JobProcessorException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
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
    private static final String QUEUE = ProcessorConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = ProcessorConfig.QUEUE.fqnAsAddress();

    public JobStoreMessageConsumer(ServiceHub serviceHub) {
        super(serviceHub);
        healthService = serviceHub.healthService;
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        chunkProcessor = new ChunkProcessor(healthService, jobStoreServiceConnector::getCachedFlow);
        Metric.dataio_jobprocessor_chunk_duration_ms.gauge(this::getLongestRunningChunkDuration);
        zombieWatch.addCheck("script-check" , this::scriptRuntimeCheck);
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
        long flowId = JMSHeader.flowId.getHeader(consumedMessage, Long.class);
        long flowVersion = JMSHeader.flowVersion.getHeader(consumedMessage, Long.class);
        String additionalArgs = JMSHeader.additionalArgs.getHeader(consumedMessage, String.class);
        sendResultToJobStore(processChunk(chunk, flowId, flowVersion, additionalArgs));
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
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

    protected Chunk extractChunkFromConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            Chunk chunk = MAPPER.readValue(consumedMessage.getMessagePayload(), Chunk.class);
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            return chunk;
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), JMSHeader.payload.getHeader(consumedMessage, String.class)), e);
        }
    }

    private Chunk processChunk(Chunk chunk, long flowId, long flowVersion, String additionalArgs) {
        WatchKey key = new WatchKey(chunk);
        Instant start = Instant.now();
        scriptStartTimes.put(key, start);
        try {
            Chunk process = chunkProcessor.process(chunk, flowId, flowVersion, additionalArgs);
            Metric.dataio_jobprocessor_chunk_failed.counter().inc(process.getItems().stream()
                    .map(ChunkItem::getStatus)
                    .filter(ChunkItem.Status.FAILURE::equals)
                    .count());
            return process;
        } finally {
            scriptStartTimes.remove(key);
            Duration duration = Duration.between(start, Instant.now());
            if(duration.compareTo(SLOW_THRESHOLD_MS) > 0) {
                Tag tag = new Tag("flow", flowId + ":" + flowVersion);
                Metric.dataio_jobprocessor_slow_jobs.simpleTimer(tag).update(duration);
            }
        }
    }

    public static class WatchKey {
        public final long chunkId;
        public final long jobId;
        public final long threadId;

        public WatchKey(Chunk chunk) {
            chunkId = chunk.getChunkId();
            jobId = chunk.getJobId();
            threadId = Thread.currentThread().getId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WatchKey watchKey = (WatchKey) o;
            return chunkId == watchKey.chunkId && jobId == watchKey.jobId && threadId == watchKey.threadId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkId, jobId, threadId);
        }
    }
}
