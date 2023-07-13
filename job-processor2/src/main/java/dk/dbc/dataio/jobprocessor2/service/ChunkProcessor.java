package dk.dbc.dataio.jobprocessor2.service;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor2.util.ChunkItemProcessor;
import dk.dbc.dataio.jobprocessor2.util.FlowCache;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in the associated flow
 */
public class ChunkProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessor.class);
    private static final String FLOW_NAME_MDC_KEY = "flowName";
    private static final String FLOW_VERSION_MDC_KEY = "flowVersion";
    private final HealthService healthService;
    private static final FlowCache flowCache = new FlowCache();

    public ChunkProcessor(HealthService healthService) {
        this.healthService = healthService;
    }

    public static void clearFlowCache() {
        flowCache.clear();
    }

    /**
     * Processes given chunk with business logic dictated by given flow
     *
     * @param chunk          chunk
     * @param flow           flow containing business logic
     * @param additionalArgs supplementary process data
     * @return result of processing
     */
    public Chunk process(Chunk chunk, Flow flow, String additionalArgs) {
        StopWatch stopWatchForChunk = new StopWatch();
        try {
            flowMdcPut(flow);

            LOGGER.info("process(): processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
            Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED);
            if (chunk.size() > 0) {
                try {
                    FlowCache.FlowCacheEntry flowCacheEntry = cacheFlow(flow);

                    result.addAllItems(
                            processItemsWithCurrentRevision(chunk, flowCacheEntry, additionalArgs),
                            processItemsWithNextRevision(chunk, flowCacheEntry, additionalArgs));
                } catch (OutOfMemoryError t) {
                        healthService.signal(HealthFlag.OUT_OF_MEMORY);
                        throw t;
                } catch (Exception e) {
                    // Since we cannot signal failure at chunk level, we have to fail all items in the chunk
                    LOGGER.error("process(): unrecoverable exception caught while processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId(), e);
                    result.addAllItems(failAllItems(chunk, e));
                }
            }
            return result;
        } finally {
            LOGGER.info("process(): processing of chunk {}/{} took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), stopWatchForChunk.getElapsedTime());
            flowMdcRemove();
        }
    }

    /**
     * Returns flow identified by given ID and version if already cached by this processor thread
     *
     * @param flowId      flow ID
     * @param flowVersion flow version
     * @return Flow instance if cached, empty if not
     */
    public Optional<Flow> getCachedFlow(long flowId, long flowVersion) {
        return Optional.ofNullable(flowCache.get(getCacheKey(flowId, flowVersion))).map(f -> f.flow);
    }

    private void flowMdcPut(Flow flow) {
        MDC.put(FLOW_NAME_MDC_KEY, flow.getContent().getName());
        MDC.put(FLOW_VERSION_MDC_KEY, String.valueOf(flow.getVersion()));
    }

    private void flowMdcRemove() {
        MDC.remove(FLOW_NAME_MDC_KEY);
        MDC.remove(FLOW_VERSION_MDC_KEY);
    }

    protected FlowCache.FlowCacheEntry cacheFlow(Flow flow) throws OutOfMemoryError, Exception {
        String cacheKey = getCacheKey(flow.getId(), flow.getVersion());
        FlowCache.FlowCacheEntry entry = flowCache.get(cacheKey);
        if (entry != null) {
            LOGGER.info("cacheFlow(): cache hit for flow (id.version) ({})", cacheKey);
            return entry;
        }
        return flowCache.put(cacheKey, flow);
    }

    private String getCacheKey(long flowId, long flowVersion) {
        return flowId + "." + flowVersion;
    }

    private List<ChunkItem> processItemsWithCurrentRevision(Chunk chunk, FlowCache.FlowCacheEntry flowCacheEntry, String additionalArgs) {
        return processItems(chunk, new ChunkItemProcessor(chunk.getJobId(), chunk.getChunkId(),
                flowCacheEntry.scripts, additionalArgs));
    }

    private List<ChunkItem> processItemsWithNextRevision(Chunk chunk, FlowCache.FlowCacheEntry flowCacheEntry, String additionalArgs) {
        if (!flowCacheEntry.next.isEmpty()) {
            return processItems(chunk, new ChunkItemProcessor(chunk.getJobId(), chunk.getChunkId(),
                    flowCacheEntry.next, additionalArgs));
        }
        return null;
    }

    /* Processes each item in given chunk in sequence */
    private List<ChunkItem> processItems(Chunk chunk, ChunkItemProcessor chunkItemProcessor) {
        List<ChunkItem> results = new ArrayList<>();
        try {
            for (ChunkItem item : chunk) {
                DBCTrackedLogContext.setTrackingId(item.getTrackingId());
                StopWatch timer = new StopWatch();
                try {
                    results.add(chunkItemProcessor.processWithRetry(item));
                } finally {
                    LOGGER.info("processItems(): javascript execution for item {}/{}/{} took {} milliseconds",
                            chunk.getJobId(), chunk.getChunkId(), item.getId(), timer.getElapsedTime());
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return results;
    }

    private List<ChunkItem> failAllItems(Chunk chunk, Throwable t) {
        List<ChunkItem> failedItems = new ArrayList<>();
        for (ChunkItem item : chunk) {
            failedItems.add(ChunkItem.failedChunkItem()
                    .withId(item.getId())
                    .withData(StringUtil.getStackTraceString(t))
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(item.getTrackingId())
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "Chunk item failed during processing", t)));
        }
        return failedItems;
    }
}
