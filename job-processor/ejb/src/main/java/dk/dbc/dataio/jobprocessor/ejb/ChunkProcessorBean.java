package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.jobprocessor.javascript.JSWrapperSingleScript;
import dk.dbc.dataio.jobprocessor.javascript.StringSourceSchemeHandler;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in
 * associated flow
 */
@Stateless
@LocalBean
public class ChunkProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessorBean.class);
    private static final int CACHE_MAX_ENTRIES = 5;

    // A per bean instance LRU cache using a LinkedHashMap with access-ordering
    private final LinkedHashMap<String, List<JSWrapperSingleScript>> flowCache =
            new LinkedHashMap(CACHE_MAX_ENTRIES + 1, .75F, true) {
                @Override
                public boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > CACHE_MAX_ENTRIES;
            }};

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

            List<ChunkItem> processedItems = new ArrayList<>(0);
            if (chunk.size() > 0) {
                try {
                    final List<JSWrapperSingleScript> jsWrappers = setupJavaScriptEnvironments(flow);
                    final Object supplementaryData = convertSupplementaryProcessDataToJsJsonObject(jsWrappers.get(0), supplementaryProcessData);
                    processedItems = processItems(chunk, supplementaryData, jsWrappers);
                } catch (Throwable ex) {
                    // Since we cannot signal failure at chunk level, we have to fail all items in the chunk
                    // with the Throwable.
                    processedItems = failItemsWithThrowable(chunk, ex);
                }
            }

            final ExternalChunk processedChunk = new ExternalChunk(chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.PROCESSED);
            processedChunk.setEncoding(Charset.defaultCharset());// todo: Change Chunk to get actual Charset
            for (ChunkItem item : processedItems) {
                processedChunk.insertItem(item);
            }
            return processedChunk;
        } finally {
            LOGGER.info("processing of chunk (jobId/chunkId) ({}/{}) took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), stopWatchForChunk.getElapsedTime());
        }
    }

    /* Creates separate javascript environments for each component in given flow and returns
       them as wrapped scripts.
     */
    private List<JSWrapperSingleScript> setupJavaScriptEnvironments(Flow flow) throws IllegalStateException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final String cacheKey = String.format("%d.%d", flow.getId(), flow.getVersion());
            if (flowCache.containsKey(cacheKey)) {
                LOGGER.info("Cache hit for flow (id.version) ({})", cacheKey);
                return flowCache.get(cacheKey);
            }

            LOGGER.info("Setting up Javascript environments");
            final List<JSWrapperSingleScript> jsWrappers = new ArrayList<>(flow.getContent().getComponents().size());

            for (FlowComponent flowComponent : flow.getContent().getComponents()) {
                final FlowComponentContent flowComponentContent = flowComponent.getContent();
                final List<JavaScript> javaScriptsBase64 = flowComponentContent.getJavascripts();
                final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>(javaScriptsBase64.size());
                for (JavaScript javascriptBase64 : javaScriptsBase64) {
                    javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(),
                            Base64Util.base64decode(javascriptBase64.getJavascript())));
                }
                String requireCacheJson = null;
                if( flowComponentContent.getRequireCache() != null ) {
                    requireCacheJson = Base64Util.base64decode(flowComponentContent.getRequireCache());
                }
                jsWrappers.add(new JSWrapperSingleScript(flowComponentContent.getName(),
                        flowComponentContent.getInvocationMethod(), javaScripts, requireCacheJson));
            }
            if (jsWrappers.isEmpty()) {
                throw new IllegalStateException(String.format("No javascript found in flow %s", flow.getContent().getName()));
            }
            flowCache.put(cacheKey, jsWrappers);

            return jsWrappers;
        } finally {
            LOGGER.debug("Javascript environments creation took {} milliseconds", stopWatch.getElapsedTime());
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
                LOGGER.info("JavaScript processing result:\n{}", data);
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
        } catch (Throwable ex) {
            LOGGER.error("Exception caught during JavaScript processing", ex);
            processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(getFailureMessage(ex)), ChunkItem.Status.FAILURE);
        }
        return processedItem;
    }

    private String invokeJavaScript(JSWrapperSingleScript jsWrapper, String data, Object supplementaryData, LogStoreTrackingId trackingId) {
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

    private Object convertSupplementaryProcessDataToJsJsonObject(JSWrapperSingleScript scriptWrapper, SupplementaryProcessData supplementaryProcessData) throws JSONBException {
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
