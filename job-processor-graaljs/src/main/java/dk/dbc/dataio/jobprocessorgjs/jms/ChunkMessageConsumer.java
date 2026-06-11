package dk.dbc.dataio.jobprocessorgjs.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobprocessorgjs.Metric;
import dk.dbc.dataio.jobprocessorgjs.health.ProcessorHealth;
import dk.dbc.dataio.jobprocessorgjs.service.ChunkProcessor;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkMessageConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkMessageConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration SLOW_THRESHOLD = Duration.ofMinutes(2);
    private static final Duration TIMEOUT_THRESHOLD = Duration.ofMinutes(3);

    private final ChunkProcessor chunkProcessor;
    private final JobStoreServiceConnector jobStoreConnector;
    private final ConcurrentHashMap<Thread, Instant> processingStartTimes = new ConcurrentHashMap<>();

    public ChunkMessageConsumer(ChunkProcessor chunkProcessor, JobStoreServiceConnector jobStoreConnector) {
        this.chunkProcessor = chunkProcessor;
        this.jobStoreConnector = jobStoreConnector;
        Metric.dataio_jobprocessor_chunk_duration_ms.gauge(this::getLongestRunningChunkDuration);
    }

    public void onMessage(Message message) throws JMSException, IOException, JobStoreServiceConnectorException {
        if (!(message instanceof TextMessage textMessage)) {
            LOGGER.warn("Ignoring non-text JMS message {}", message.getJMSMessageID());
            return;
        }

        Chunk chunk = MAPPER.readValue(textMessage.getText(), Chunk.class);
        if (chunk.getType() != Chunk.Type.PARTITIONED) {
            throw new IllegalArgumentException(
                    "Unexpected chunk type " + chunk.getType() + " for chunk " + chunk.getChunkId());
        }
        long flowId = JMSHeader.flowId.getHeader(message, Long.class);
        long flowVersion = JMSHeader.flowVersion.getHeader(message, Long.class);
        String additionalArgs = JMSHeader.additionalArgs.getHeader(message, String.class);

        LOGGER.info("Received chunk {}/{} flowId={}", chunk.getJobId(), chunk.getChunkId(), flowId);

        Thread currentThread = Thread.currentThread();
        Instant start = Instant.now();
        processingStartTimes.put(currentThread, start);
        try {
            Chunk processed = chunkProcessor.process(chunk, flowId, flowVersion, additionalArgs);
            Metric.dataio_jobprocessor_chunk_failed.counter().inc(processed.getItems().stream()
                    .map(ChunkItem::getStatus)
                    .filter(ChunkItem.Status.FAILURE::equals)
                    .count());
            jobStoreConnector.addChunkIgnoreDuplicates(processed, processed.getJobId(), processed.getChunkId());
        } finally {
            processingStartTimes.remove(currentThread);
            Duration duration = Duration.between(start, Instant.now());
            if (duration.compareTo(SLOW_THRESHOLD) > 0) {
                Metric.dataio_jobprocessor_slow_jobs.timer(new Tag("flow", flowId + ":" + flowVersion))
                        .update(duration);
            }
        }
    }

    public void checkTimeouts(ProcessorHealth health) {
        Instant now = Instant.now();
        processingStartTimes.entrySet().stream()
                .filter(e -> Duration.between(e.getValue(), now).compareTo(TIMEOUT_THRESHOLD) > 0)
                .findFirst()
                .ifPresent(e -> {
                    LOGGER.error("Processing on thread '{}' has exceeded {} — signalling health failure",
                            e.getKey().getName(), TIMEOUT_THRESHOLD);
                    health.signalFatal("Timeout: chunk processing exceeded " + TIMEOUT_THRESHOLD);
                });
    }

    private long getLongestRunningChunkDuration() {
        long now = System.currentTimeMillis();
        return processingStartTimes.values().stream()
                .mapToLong(t -> now - t.toEpochMilli())
                .max()
                .orElse(0);
    }
}
