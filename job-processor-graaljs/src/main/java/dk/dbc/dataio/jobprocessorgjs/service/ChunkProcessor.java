package dk.dbc.dataio.jobprocessorgjs.service;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessorgjs.health.ProcessorHealth;
import dk.dbc.dataio.jobprocessorgjs.logstore.LogStoreWriter;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChunkProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessor.class);
    private static final String FLOW_NAME_MDC_KEY = "flowName";
    private static final String FLOW_VERSION_MDC_KEY = "flowVersion";

    private final FlowCache flowCache;
    private final ProcessorHealth health;
    private final FlowFetcher flowFetcher;
    private final LogStoreWriter logStoreWriter;

    public ChunkProcessor(ProcessorHealth health, FlowCache flowCache, FlowFetcher flowFetcher,
                          LogStoreWriter logStoreWriter) {
        this.health = health;
        this.flowCache = flowCache;
        this.flowFetcher = flowFetcher;
        this.logStoreWriter = logStoreWriter;
    }

    public Chunk process(Chunk chunk, long flowId, long flowVersion, String additionalArgs) {
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED);
        long start = System.currentTimeMillis();
        try {
            if (!chunk.isEmpty()) {
                FlowCache.FlowCacheEntry entry = getFlow(chunk, flowId, flowVersion);
                mdcPut(entry.flow);
                LOGGER.info("process(): processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
                try {
                    result.addAllItems(processItems(chunk, entry, additionalArgs), null);
                } catch (OutOfMemoryError e) {
                    health.signalOutOfMemory();
                    throw e;
                }
            }
            return result;
        } catch (ExecutionException e) {
            // Flow/JSAR could not be fetched — infrastructure failure, not a script error.
            // Throw so the JMS transaction is rolled back and the chunk is retried.
            throw new RuntimeException("Flow fetch failed for chunk "
                    + chunk.getJobId() + "/" + chunk.getChunkId(), e);
        } catch (Exception e) {
            LOGGER.error("process(): unrecoverable exception for chunk {}/{}",
                    chunk.getJobId(), chunk.getChunkId(), e);
            result.addAllItems(failAllItems(chunk, e));
            return result;
        } finally {
            LOGGER.info("process(): chunk {}/{} took {} ms",
                    chunk.getJobId(), chunk.getChunkId(), System.currentTimeMillis() - start);
            mdcRemove();
        }
    }

    private FlowCache.FlowCacheEntry getFlow(Chunk chunk, long flowId, long flowVersion)
            throws ExecutionException {
        String key = flowId + "." + flowVersion;
        return flowCache.get(key, () -> {
            try {
                return flowFetcher.fetch(chunk.getJobId());
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to fetch flow for job " + chunk.getJobId(), e);
            }
        });
    }

    private List<ChunkItem> processItems(Chunk chunk, FlowCache.FlowCacheEntry entry,
                                         String additionalArgs) {
        ChunkItemProcessor itemProcessor = new ChunkItemProcessor(
                chunk.getJobId(), chunk.getChunkId(), entry.script, additionalArgs, logStoreWriter);
        List<ChunkItem> results = new ArrayList<>();
        try {
            for (ChunkItem item : chunk) {
                DBCTrackedLogContext.setTrackingId(item.getTrackingId());
                long start = System.currentTimeMillis();
                try {
                    results.add(itemProcessor.process(item));
                } finally {
                    LOGGER.info("processItems(): item {}/{}/{} took {} ms",
                            chunk.getJobId(), chunk.getChunkId(), item.getId(),
                            System.currentTimeMillis() - start);
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return results;
    }

    private List<ChunkItem> failAllItems(Chunk chunk, Throwable t) {
        List<ChunkItem> failed = new ArrayList<>();
        for (ChunkItem item : chunk) {
            failed.add(ChunkItem.failedChunkItem()
                    .withId(item.getId())
                    .withData(StringUtil.getStackTraceString(t))
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(item.getTrackingId())
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL,
                            "Chunk item failed during processing", t)));
        }
        return failed;
    }

    private void mdcPut(Flow flow) {
        MDC.put(FLOW_NAME_MDC_KEY, flow.getContent().getName());
        MDC.put(FLOW_VERSION_MDC_KEY, String.valueOf(flow.getVersion()));
    }

    private void mdcRemove() {
        MDC.remove(FLOW_NAME_MDC_KEY);
        MDC.remove(FLOW_VERSION_MDC_KEY);
    }

    public interface FlowFetcher {
        Flow fetch(int jobId) throws Exception;
    }
}
