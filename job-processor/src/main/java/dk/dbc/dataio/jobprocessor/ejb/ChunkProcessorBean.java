/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor.util.ChunkItemProcessor;
import dk.dbc.dataio.jobprocessor.util.FlowCache;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in the associated flow
 */
@Stateless
@LocalBean
public class ChunkProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessorBean.class);
    private static final String FLOW_NAME_MDC_KEY = "flowName";
    private static final String FLOW_VERSION_MDC_KEY = "flowVersion";

    @EJB
    HealthBean healthBean;

    // A per bean instance LRU flow cache
    private final FlowCache flowCache = new FlowCache();

    /**
     * Processes given chunk with business logic dictated by given flow
     * @param chunk chunk
     * @param flow flow containing business logic
     * @param additionalArgs supplementary process data
     * @return result of processing
     */
    public Chunk process(Chunk chunk, Flow flow, String additionalArgs) {
        final StopWatch stopWatchForChunk = new StopWatch();
        try {
            flowMdcPut(flow);

            LOGGER.info("process(): processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
            final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED);
            if (chunk.size() > 0) {
                try {
                    final FlowCache.FlowCacheEntry flowCacheEntry = cacheFlow(flow);

                    result.addAllItems(
                            processItemsWithCurrentRevision(chunk, flowCacheEntry, additionalArgs),
                            processItemsWithNextRevision(chunk, flowCacheEntry, additionalArgs));
                } catch (Throwable t) {
                    // bug 20964: a ClassCastException ("java.lang.invoke.LambdaForm cannot be cast to [Ljava.lang.invoke.LambdaFormEditor$Transform")
                    // has been encountered here which makes the job processor
                    // fail in an unrecoverable manner. The current strategy is to restart the application.
                    // http://bugs.dbc.dk/show_bug.cgi?id=20964
                    // https://bugs.openjdk.java.net/browse/JDK-8145371
                    if (t instanceof ClassCastException
                            || t.getCause() != null && t.getCause() instanceof ClassCastException) {
                        LOGGER.error("Processor reported itself terminally ill");
                        healthBean.signalTerminallyIll(t);
                        throw new RuntimeException("Processor reported itself terminally ill");
                    }
                    // Since we cannot signal failure at chunk level, we have to fail all items in the chunk
                    LOGGER.error("process(): unrecoverable exception caught while processing chunk {}/{}", chunk.getJobId(), chunk.getChunkId(), t);
                    result.addAllItems(failAllItems(chunk, t));
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
     * @param flowId flow ID
     * @param flowVersion flow version
     * @return Flow instance if cached, empty if not
     */
    public Optional<Flow> getCachedFlow(long flowId, long flowVersion) {
        final FlowCache.FlowCacheEntry flowCacheEntry = flowCache.get(getCacheKey(flowId, flowVersion));
        if (flowCacheEntry != null) {
            return Optional.of(flowCacheEntry.flow);
        }
        return Optional.empty();
    }

    private void flowMdcPut(Flow flow) {
        MDC.put(FLOW_NAME_MDC_KEY, flow.getContent().getName());
        MDC.put(FLOW_VERSION_MDC_KEY, String.valueOf(flow.getVersion()));
    }

    private void flowMdcRemove() {
        MDC.remove(FLOW_NAME_MDC_KEY);
        MDC.remove(FLOW_VERSION_MDC_KEY);
    }

    protected FlowCache.FlowCacheEntry cacheFlow(Flow flow) throws Throwable {
        final String cacheKey = getCacheKey(flow.getId(), flow.getVersion());
        if (flowCache.containsKey(cacheKey)) {
            LOGGER.info("cacheFlow(): cache hit for flow (id.version) ({})", cacheKey);
            return flowCache.get(cacheKey);
        }
        return flowCache.put(cacheKey, flow);
    }

    private String getCacheKey(long flowId, long flowVersion) {
        return String.format("%d.%d", flowId, flowVersion);
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
        final List<ChunkItem> results = new ArrayList<>();
        try {
            for (ChunkItem item : chunk) {
                DBCTrackedLogContext.setTrackingId(item.getTrackingId());
                final StopWatch timer = new StopWatch();
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
        final List<ChunkItem> failedItems = new ArrayList<>();
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
