package dk.dbc.dataio.jobprocessor2.service;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.DataIOConnectorException;
import dk.dbc.dataio.jobprocessor2.ProcessorConfig;
import dk.dbc.dataio.jobprocessor2.util.ChunkItemProcessor;
import dk.dbc.dataio.jobprocessor2.util.FlowCache;
import dk.dbc.dataio.jse.artemis.common.JobProcessorException;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in the associated flow
 */
public class ChunkProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessor.class);
    private static final String FLOW_NAME_MDC_KEY = "flowName";
    private static final String FLOW_VERSION_MDC_KEY = "flowVersion";
    private static final AtomicInteger PROCESSOR_ID_GEN = new AtomicInteger();
    private final int processorId = PROCESSOR_ID_GEN.getAndIncrement();
    private static final boolean SHARE_FLOWS = ProcessorConfig.SHARE_FLOWS.asBoolean();
    private final HealthService healthService;
    private static final FlowCache flowCache = new FlowCache();
    private final FlowFetcher flowFetcher;

    public ChunkProcessor(HealthService healthService, FlowFetcher flowFetcher) {
        this.healthService = healthService;
        this.flowFetcher = flowFetcher;
    }

    public static void clearFlowCache() {
        flowCache.clear();
    }

    public Map<String, FlowCache.FlowCacheEntry> getCacheView() {
        return flowCache.getView();
    }

    /**
     * Processes given chunk with business logic dictated by given flow
     *
     * @param chunk          chunk
     * @param flowId         flowId containing business logic
     * @param flowVersion    flowVersion containing business logic
     * @param additionalArgs supplementary process data
     * @return result of processing
     */
    public Chunk process(Chunk chunk, long flowId, long flowVersion, String additionalArgs) {
        StopWatch stopWatchForChunk = new StopWatch();
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED);
        try {
            if (chunk.size() > 0) {
                FlowCache.FlowCacheEntry flowCacheEntry = getFlow(chunk, flowId, flowVersion);
                flowMdcPut(flowCacheEntry.flow);
                LOGGER.info("process(): processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

                try {
                    result.addAllItems(
                            processItemsWithCurrentRevision(chunk, flowCacheEntry, additionalArgs),
                            processItemsWithNextRevision(chunk, flowCacheEntry, additionalArgs));
                } catch (OutOfMemoryError t) {
                        healthService.signal(HealthFlag.OUT_OF_MEMORY);
                        throw t;
                }
            }
            return result;
        } catch (Exception e) {
            // Since we cannot signal failure at chunk level, we have to fail all items in the chunk
            LOGGER.error("process(): unrecoverable exception caught while processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId(), e);
            result.addAllItems(failAllItems(chunk, e));
            return result;
        } finally {
            LOGGER.info("process(): processing of chunk {}/{} took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), stopWatchForChunk.getElapsedTime());
            flowMdcRemove();
        }
    }

    private FlowCache.FlowCacheEntry getFlow(Chunk chunk, long flowId, long flowVersion) throws Exception {
        String cacheKey = getCacheKey(flowId, flowVersion);
        return flowCache.get(cacheKey, () -> flowFromJobStore(chunk));
    }

    private Flow flowFromJobStore(Chunk chunk) throws JobProcessorException {
        StopWatch stopWatch = new StopWatch();
        try {
            return flowFetcher.fetch(chunk.getJobId());
        } catch (DataIOConnectorException e) {
            throw new JobProcessorException(String.format(
                    "Exception caught while fetching flow for job %s", chunk.getJobId()), e);
        } finally {
            LOGGER.debug("Fetching flow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private void flowMdcPut(Flow flow) {
        MDC.put(FLOW_NAME_MDC_KEY, flow.getContent().getName());
        MDC.put(FLOW_VERSION_MDC_KEY, String.valueOf(flow.getVersion()));
    }

    private void flowMdcRemove() {
        MDC.remove(FLOW_NAME_MDC_KEY);
        MDC.remove(FLOW_VERSION_MDC_KEY);
    }

    private String getCacheKey(long flowId, long flowVersion) {
        return (!SHARE_FLOWS ? "p" + processorId + ":" : "") + flowId + "." + flowVersion;
    }

    private List<ChunkItem> processItemsWithCurrentRevision(Chunk chunk, FlowCache.FlowCacheEntry flowCacheEntry, String additionalArgs) {
        return processItems(chunk, new ChunkItemProcessor(chunk.getJobId(), chunk.getChunkId(),
                flowCacheEntry.scripts, additionalArgs));
    }

    protected List<ChunkItem> processItemsWithNextRevision(Chunk chunk, FlowCache.FlowCacheEntry flowCacheEntry, String additionalArgs) {
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

    public interface FlowFetcher {
        Flow fetch(int id) throws DataIOConnectorException;
    }
}
