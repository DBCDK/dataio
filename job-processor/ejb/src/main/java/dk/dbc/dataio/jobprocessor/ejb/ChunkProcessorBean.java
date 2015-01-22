package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.jobprocessor.javascript.JSWrapperSingleScript;
import dk.dbc.dataio.jobprocessor.javascript.StringSourceSchemeHandler;
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
import java.util.List;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in
 * associated flow
 */
@Stateless
@LocalBean
public class ChunkProcessorBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessorBean.class);

    /**
     * Processes given chunk
     *
     * @param chunk chunk
     * @param flow flow
     *
     * @return result of processing
     */
    public ExternalChunk process(ExternalChunk chunk, Flow flow, SupplementaryProcessData supplementaryProcessData) {
        final StopWatch stopWatchForChunk = new StopWatch();
        LOGGER.info("Processing chunk {} in job {}", chunk.getChunkId(), chunk.getJobId());
        // final Flow flow = chunk.getFlow();
        List<ChunkItem> processedItems = new ArrayList<>();

        if (chunk.size() > 0) {
            try {
                JSWrapperSingleScript scriptWrapper = setupJavaScriptEnvironment(flow);
                processedItems = processItemsWithLogStoreLogging(chunk, flow, supplementaryProcessData, scriptWrapper);
            } catch (Throwable ex) {
                // Since we cannot have a failed chunk, we have to fail all items in the chunk
                // with the Throwable from the javascript-enviroment failure.
                processedItems = failItemsWithThrowable(chunk, ex);
            }
        }

        ExternalChunk processedChunk = new ExternalChunk(chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.PROCESSED);
        processedChunk.setEncoding(Charset.defaultCharset());// todo: Change Chunk to get actual Charset
        for(ChunkItem item : processedItems) {
            processedChunk.insertItem(item);
        }
        LOGGER.info("processing of chunk (jobId/chunkId) ({}/{}) took {} milliseconds", chunk.getJobId(), chunk.getChunkId(), stopWatchForChunk.getElapsedTime());
        return processedChunk;
    }

    private JSWrapperSingleScript setupJavaScriptEnvironment(Flow flow) {
        LOGGER.info("Setting up Javascript environment");
        final StopWatch stopWatch = new StopWatch();

        final List<JavaScript> javaScriptsBase64 = flow.getContent().getComponents().get(0).getContent().getJavascripts();
        final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>();
        for (JavaScript javascriptBase64 : javaScriptsBase64) {
            javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(), Base64Util.base64decode(javascriptBase64.getJavascript())));
        }
        JSWrapperSingleScript scriptWrapper = new JSWrapperSingleScript(javaScripts);

        LOGGER.debug("Javascript environment creation took {} milliseconds", stopWatch.getElapsedTime());
        return scriptWrapper;
    }

    private List<ChunkItem> processItemsWithLogStoreLogging(ExternalChunk chunk, Flow flow, SupplementaryProcessData supplementaryProcessData, JSWrapperSingleScript scriptWrapper) {
        List<ChunkItem> processedItems = new ArrayList<>();
        for (ChunkItem item : chunk) {
            final StopWatch stopWatchForItem = new StopWatch();
            ChunkItem processedItem = setupLogStoreLoggingAndProcessItem(flow, supplementaryProcessData, scriptWrapper, item, chunk);
            processedItems.add(processedItem);
            LOGGER.info("Javascript execution for (job/chunk/item) ({}/{}/{}) took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), item.getId(), stopWatchForItem.getElapsedTime());
        }
        return processedItems;
    }

    private ChunkItem setupLogStoreLoggingAndProcessItem(Flow flow, SupplementaryProcessData supplementaryProcessData, JSWrapperSingleScript scriptWrapper, ChunkItem inputItem, ExternalChunk chunk) {
        ChunkItem processedItem;
        try {
            MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY,
                    LogStoreTrackingId.create(String.valueOf(chunk.getJobId()), chunk.getChunkId(), inputItem.getId()).toString());
            processedItem = processItem(flow, scriptWrapper, inputItem, supplementaryProcessData);
        } finally {
            MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY, "true");
            // This timing assumes the use of LogStoreBufferedJdbcAppender to be meaningful
            final StopWatch stopWatchForLogStoreBatch = new StopWatch();
            LOGGER.info("Done");
            MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY);
            MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
            LOGGER.info("LogStore batch insert for (job/chunk/item) ({}/{}/{}) took {} milliseconds",
                    chunk.getJobId(), chunk.getChunkId(), inputItem.getId(), stopWatchForLogStoreBatch.getElapsedTime());
        }
        return processedItem;
    }

    private ChunkItem processItem(final Flow flow, JSWrapperSingleScript scriptWrapper, ChunkItem inputItem, SupplementaryProcessData supplementaryProcessData) {
        ChunkItem processedItem;
        try {
            final String processedRecord = invokeJavaScript(flow, scriptWrapper, Base64Util.base64decode(inputItem.getData()), supplementaryProcessData);
            LOGGER.info("JavaScript processing result:\n{}", processedRecord);
            processedItem = new ChunkItem(inputItem.getId(), Base64Util.base64encode(processedRecord), ChunkItem.Status.SUCCESS);
        } catch (Throwable ex) {
            LOGGER.error("Exception caught during JavaScript processing", ex);
            final String failureMsg = getFailureMessage(ex);
            processedItem = new ChunkItem(inputItem.getId(), Base64Util.base64encode(failureMsg), ChunkItem.Status.FAILURE);
        }
        return processedItem;
    }

    private String invokeJavaScript(Flow flow, JSWrapperSingleScript scriptWrapper, String record, SupplementaryProcessData supplementaryProcessData) throws JsonException {
        Object supProcDataJson = convertSupplementaryProcessDataToJsJsonObject(scriptWrapper, supplementaryProcessData);
        LOGGER.trace("Starting javascript with invocation method: [{}]", flow.getContent().getComponents().get(0).getContent().getInvocationMethod());
        final Object result = scriptWrapper.callMethod(flow.getContent().getComponents().get(0).getContent().getInvocationMethod(), new Object[]{record, supProcDataJson});
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

    private Object convertSupplementaryProcessDataToJsJsonObject(JSWrapperSingleScript scriptWrapper, SupplementaryProcessData supplementaryProcessData) throws JsonException {
        // Something about why you need parantheses in the string around the json
        // when trying to evalute the json in javascript (rhino):
        // http://rayfd.me/2007/03/28/why-wont-eval-eval-my-json-or-json-object-object-literal/
        String jsonStr = "(" + JsonUtil.toJson(supplementaryProcessData) + ")"; // notice the parantheses!
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
