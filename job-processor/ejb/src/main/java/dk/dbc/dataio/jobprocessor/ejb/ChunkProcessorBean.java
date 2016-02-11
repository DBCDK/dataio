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
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor.javascript.JSWrapperSingleScript;
import dk.dbc.dataio.jobprocessor.util.FlowCache;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.javascript.recordprocessing.IgnoreRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in
 * associated flow
 */
@Stateless
@LocalBean
public class ChunkProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessorBean.class);
    private static final String FLOW_NAME_MDC_KEY = "flowName";
    private static final String FLOW_VERSION_MDC_KEY = "flowVersion";

    // A per bean instance LRU flow cache
    private final FlowCache flowCache = new FlowCache();

    JSONBContext jsonbContext = new JSONBContext();

    /**
     * Processes given chunk with business logic dictated by given flow
     * @param chunk chunk
     * @param flow flow containing business logic
     * @param supplementaryProcessData supplementary process data
     * @return result of processing
     */
    public Chunk process(Chunk chunk, Flow flow, SupplementaryProcessData supplementaryProcessData) {
        final StopWatch stopWatchForChunk = new StopWatch();
        try {
            MDC.put(FLOW_NAME_MDC_KEY, flow.getContent().getName());
            MDC.put(FLOW_VERSION_MDC_KEY, String.valueOf(flow.getVersion()));
            LOGGER.info("Processing chunk {} in job {}", chunk.getChunkId(), chunk.getJobId());
            final Chunk processedChunk = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED);
            processedChunk.setEncoding(Charset.defaultCharset());// todo: Remove once Chunk gets encoding from ChunkItem
            if (chunk.size() > 0) {
                try {
                    final FlowCache.FlowCacheEntry flowCacheEntry = cacheFlow(flow);
                    final Object supplementaryData = convertSupplementaryProcessDataToJsJsonObject(flowCacheEntry.scripts.get(0), supplementaryProcessData);
                    final List<ChunkItem> items = processItems(chunk, supplementaryData, flowCacheEntry.scripts);
                    List<ChunkItem> next = null;
                    if (!flowCacheEntry.next.isEmpty()) {
                        next = processItems(chunk, supplementaryData, flowCacheEntry.next);
                    }
                    processedChunk.addAllItems(items, next);
                } catch (Throwable t) {
                    // Since we cannot signal failure at chunk level, we have to fail all items in the chunk
                    // with the Throwable.
                    processedChunk.addAllItems(failItemsWithThrowable(chunk, t));
                }
            }
            return processedChunk;
        } finally {
            LOGGER.info("processing of chunk (jobId/chunkId) ({}/{}) took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), stopWatchForChunk.getElapsedTime());
            MDC.remove(FLOW_NAME_MDC_KEY);
            MDC.remove(FLOW_VERSION_MDC_KEY);
        }
    }

    private FlowCache.FlowCacheEntry cacheFlow(Flow flow) throws Throwable {
        final StopWatch stopWatch = new StopWatch();
        try {
            String cacheKey = String.format("%d.%d", flow.getId(), flow.getVersion());
            if (flow.hasNextComponents()) {
                // Avoids mixing up acctest and non-acctest flows in cache.
                cacheKey += ".acctest";
            }
            if (flowCache.containsKey(cacheKey)) {
                LOGGER.info("Cache hit for flow (id.version) ({})", cacheKey);
                return flowCache.get(cacheKey);
            }
            return flowCache.put(cacheKey, flow);
        } finally {
            LOGGER.debug("Flow caching took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /* Processes each item in given chunk in sequence
     */
    private List<ChunkItem> processItems(Chunk chunk, Object supplementaryData, List<JSWrapperSingleScript> jsWrappers) {
        List<ChunkItem> processedItems = new ArrayList<>();
        try {
            for (ChunkItem item : chunk) {
                DBCTrackedLogContext.setTrackingId("traceid:" + item.getTrackingId());
                final StopWatch stopWatchForItem = new StopWatch();

                if (item.getStatus() != ChunkItem.Status.SUCCESS) {
                    processedItems.add(skipItem(item));
                    LOGGER.info("processing of item (jobId/chunkId/itemId) ({}/{}/{}) skipped since item.Status was {}",
                            chunk.getJobId(), chunk.getChunkId(), item.getId(), item.getStatus());
                } else {
                    try {
                        final LogStoreTrackingId logStoreTrackingId = LogStoreTrackingId.create(
                                String.valueOf(chunk.getJobId()), chunk.getChunkId(), item.getId());
                        final ChunkItem processedItem = processItemWithLogStoreTracking(item, logStoreTrackingId,
                                supplementaryData, jsWrappers);
                        processedItems.add(processedItem);
                    } finally {
                        LOGGER.info("Javascript execution for (job/chunk/item) ({}/{}/{}) took {} milliseconds",
                                chunk.getJobId(), chunk.getChunkId(), item.getId(), stopWatchForItem.getElapsedTime());
                    }
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return processedItems;
    }

    /* Processes given item with log-store tracking enabled
     */
    private ChunkItem processItemWithLogStoreTracking(ChunkItem item, LogStoreTrackingId trackingId,
            Object supplementaryData, List<JSWrapperSingleScript> jsWrappers) {
        ChunkItem processedItem;
        try {
            MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY, trackingId.toString());
            processedItem = processItem(item, supplementaryData, jsWrappers, trackingId);
        } finally {
            MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY, "true");
            // This timing assumes the use of LogStoreBufferedJdbcAppender to be meaningful
            final StopWatch stopWatchForLogStoreBatch = new StopWatch();
            LOGGER.info("Done");
            MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY);
            MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
            LOGGER.info("LogStore batch insert for (job/chunk/item) ({}/{}/{}) took {} milliseconds",
                    trackingId.getJobId(), trackingId.getChunkId(), trackingId.getItemId(), stopWatchForLogStoreBatch.getElapsedTime());
        }
        return processedItem;
    }

    /*
     * Skips javascript processing of an item and sets status to ignore.
     */
    private ChunkItem skipItem(ChunkItem item) {
         return ObjectFactory.buildIgnoredChunkItem(item.getId(), String.format("Ignored by job-processor since returned status was {%s}", item.getStatus()), item.getTrackingId());
    }

    /* Processes given item
     */
    private ChunkItem processItem(ChunkItem item, Object supplementaryData, List<JSWrapperSingleScript> jsWrappers, LogStoreTrackingId trackingId) {
        ChunkItem processedItem;
        try {
            String data = StringUtil.asString(item.getData());
            for (JSWrapperSingleScript jsWrapper : jsWrappers) {
                data = invokeJavaScript(jsWrapper, data, supplementaryData, trackingId);
                if (data.isEmpty()) {
                    // terminate pipeline processing
                    break;
                }
            }
            if (data.isEmpty()) {
                processedItem = ObjectFactory.buildIgnoredChunkItem(item.getId(), "Ignored by job-processor since returned data was empty", item.getTrackingId());
            } else {
                processedItem = ObjectFactory.buildSuccessfulChunkItem(item.getId(), data, ChunkItem.Type.UNKNOWN, item.getTrackingId());
            }
        } catch (IgnoreRecord e) {
            LOGGER.error("Record Ignored by JS with Message: {}", e.getMessage());
            processedItem = ObjectFactory.buildIgnoredChunkItem(item.getId(), e.getMessage(), item.getTrackingId());
        } catch (FailRecord e) {
            LOGGER.error("RecordProcessing Terminated by JS with Message: {}", e.getMessage());
            processedItem = ObjectFactory.buildFailedChunkItem(item.getId(), e.getMessage(), item.getTrackingId());
            processedItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(e.getMessage()));
        } catch (Throwable t) {
            LOGGER.error("Exception caught during JavaScript processing", t);
            processedItem = ObjectFactory.buildFailedChunkItem(item.getId(), StringUtil.getStackTraceString(t), item.getTrackingId());
            processedItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Exception caught during JavaScript processing", t));
        }
        return processedItem;
    }

    private String invokeJavaScript(JSWrapperSingleScript jsWrapper, String data, Object supplementaryData, LogStoreTrackingId trackingId) throws Throwable {
        LOGGER.info("Starting javascript [{}] with invocation method: [{}] and logging ID [{}]", jsWrapper.getScriptId(), jsWrapper.getInvocationMethod(), trackingId.toString());
        final Object result = jsWrapper.invoke(new Object[]{data, supplementaryData});
        return (String) result;
    }

    private Object convertSupplementaryProcessDataToJsJsonObject(JSWrapperSingleScript scriptWrapper, SupplementaryProcessData supplementaryProcessData) throws Throwable {
        // Something about why you need parentheses in the string around the json
        // when trying to evaluate the json in javascript (rhino):
        // http://rayfd.me/2007/03/28/why-wont-eval-eval-my-json-or-json-object-object-literal/
        final String jsonStr = "(" + jsonbContext.marshall(supplementaryProcessData) + ")"; // notice the parentheses!
        return scriptWrapper.eval(jsonStr);
    }

    private List<ChunkItem> failItemsWithThrowable(Chunk chunk, Throwable t) {
        final List<ChunkItem> failedItems = new ArrayList<>();
        for (ChunkItem item : chunk) {
            final ChunkItem processedChunkItem = ObjectFactory.buildFailedChunkItem(item.getId(), StringUtil.getStackTraceString(t), item.getTrackingId());
            processedChunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Chunk item failed during processing", t));
            failedItems.add(processedChunkItem);
        }
        return failedItems;
    }
}
