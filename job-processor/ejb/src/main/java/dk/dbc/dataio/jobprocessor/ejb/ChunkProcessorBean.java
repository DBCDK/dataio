package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.jobprocessor.javascript.JSWrapperSingleScript;
import dk.dbc.dataio.jobprocessor.util.FlowCache;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.javascript.recordprocessing.IgnoreRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
    public ExternalChunk process(ExternalChunk chunk, Flow flow, SupplementaryProcessData supplementaryProcessData) {
        final StopWatch stopWatchForChunk = new StopWatch();
        try {
            LOGGER.info("Processing chunk {} in job {}", chunk.getChunkId(), chunk.getJobId());
            final ExternalChunk processedChunk = new ExternalChunk(chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.PROCESSED);
            processedChunk.setEncoding(Charset.defaultCharset());// todo: Change Chunk to get actual Charset
            if (chunk.size() > 0) {
                try {
                    final FlowCache.FlowCacheEntry flowCacheEntry = cacheFlow(flow);
                    final Object supplementaryData = convertSupplementaryProcessDataToJsJsonObject(flowCacheEntry.scripts.get(0), supplementaryProcessData);
                    processedChunk.addAllItems(processItems(chunk, supplementaryData, flowCacheEntry.scripts));
                } catch (Throwable ex) {
                    // Since we cannot signal failure at chunk level, we have to fail all items in the chunk
                    // with the Throwable.
                    processedChunk.addAllItems(failItemsWithThrowable(chunk, ex));
                }
            }
            return processedChunk;
        } finally {
            LOGGER.info("processing of chunk (jobId/chunkId) ({}/{}) took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), stopWatchForChunk.getElapsedTime());
        }
    }

    private FlowCache.FlowCacheEntry cacheFlow(Flow flow) throws Throwable {
        final StopWatch stopWatch = new StopWatch();
        try {
            final String cacheKey = String.format("%d.%d", flow.getId(), flow.getVersion());
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
    private List<ChunkItem> processItems(ExternalChunk chunk, Object supplementaryData, List<JSWrapperSingleScript> jsWrappers) {
        List<ChunkItem> processedItems = new ArrayList<>();
        for (ChunkItem item : chunk) {
            final StopWatch stopWatchForItem = new StopWatch();
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

    /* Processes given item
     */
    private ChunkItem processItem(ChunkItem item, Object supplementaryData, List<JSWrapperSingleScript> jsWrappers, LogStoreTrackingId trackingId) {
        ChunkItem processedItem;
        try {
            String data = Base64Util.base64decode(item.getData());
            for (JSWrapperSingleScript jsWrapper : jsWrappers) {
                data = invokeJavaScript(jsWrapper, data, supplementaryData, trackingId);
                if (data.isEmpty()) {
                    // terminate pipeline processing
                    break;
                }
            }
            if (data.isEmpty()) {
                processedItem = new ChunkItem(item.getId(), "Ignored by job-processor since returned data was empty", ChunkItem.Status.IGNORE);
            } else {
                processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(data), ChunkItem.Status.SUCCESS);
            }
        } catch (IgnoreRecord ex ) {
            LOGGER.error("Record Ignored by JS with Message: {}", ex.getMessage());
            processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(ex.getMessage()), ChunkItem.Status.IGNORE);
        } catch (FailRecord ex) {
            LOGGER.error("RecordProcessing Terminated by JS with Message: {}", ex.getMessage());
            processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(getFailureMessage(ex)), ChunkItem.Status.FAILURE);
        } catch (Throwable ex) {
            LOGGER.error("Exception caught during JavaScript processing", ex);
            processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(getFailureMessage(ex)), ChunkItem.Status.FAILURE);
        }
        return processedItem;
    }

    private String invokeJavaScript(JSWrapperSingleScript jsWrapper, String data, Object supplementaryData, LogStoreTrackingId trackingId) throws Throwable {
        LOGGER.info("Starting javascript [{}] with invocation method: [{}] and logging ID [{}]", jsWrapper.getScriptId(), jsWrapper.getInvocationMethod(), trackingId.toString());
        final Object result = jsWrapper.invoke(new Object[]{data, supplementaryData});
        return (String) result;
    }

    private String getFailureMessage(Throwable ex) {
        String failureMsg;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
            ex.printStackTrace(ps);
            failureMsg = baos.toString("UTF-8");
        } catch (IOException e) {
            failureMsg = e.getMessage();
        }
        return failureMsg;
    }

    private Object convertSupplementaryProcessDataToJsJsonObject(JSWrapperSingleScript scriptWrapper, SupplementaryProcessData supplementaryProcessData) throws Throwable {
        // Something about why you need parentheses in the string around the json
        // when trying to evaluate the json in javascript (rhino):
        // http://rayfd.me/2007/03/28/why-wont-eval-eval-my-json-or-json-object-object-literal/
        final String jsonStr = "(" + jsonbContext.marshall(supplementaryProcessData) + ")"; // notice the parentheses!
        return scriptWrapper.eval(jsonStr);
    }

    private List<ChunkItem> failItemsWithThrowable(ExternalChunk chunk, Throwable ex) {
        List<ChunkItem> failedItems = new ArrayList<>();
        for (ChunkItem item : chunk) {
            failedItems.add(new ChunkItem(item.getId(), Base64Util.base64encode(getFailureMessage(ex)), ChunkItem.Status.FAILURE));
        }
        return failedItems;
    }
}
