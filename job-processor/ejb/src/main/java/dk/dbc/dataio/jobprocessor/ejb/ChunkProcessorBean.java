package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
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
     *
     * @return result of processing
     */
    public ChunkResult process(Chunk chunk) {
        LOGGER.info("Processing chunk {} in job {}", chunk.getChunkId(), chunk.getJobId());
        final Flow flow = chunk.getFlow();
        final List<ChunkItem> processedItems = new ArrayList<>();
        for (ChunkItem item : chunk.getItems()) {
            try {
                MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY,
                        LogStoreTrackingId.create(String.valueOf(chunk.getJobId()), chunk.getChunkId(), item.getId()).toString());

                ChunkItem processedItem;
                try {
                    final String processedRecord = invokeJavaScript(flow, Base64Util.base64decode(item.getData()), chunk.getSupplementaryProcessData());
                    LOGGER.info("JavaScript processing result:\n{}", processedRecord);
                    processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(processedRecord), ChunkItem.Status.SUCCESS);
                } catch (Throwable ex) {
                    LOGGER.error("Exception caught during JavaScript processing", ex);
                    final String failureMsg = getFailureMessage(ex);
                    processedItem = new ChunkItem(item.getId(), Base64Util.base64encode(failureMsg), ChunkItem.Status.FAILURE);
                }
                processedItems.add(processedItem);

            } finally {
                MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
            }
        }
        // todo: Change Chunk to get actual Charset
        return new ChunkResult(chunk.getJobId(), chunk.getChunkId(), Charset.defaultCharset(), processedItems);
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

    private String invokeJavaScript(Flow flow, String record, SupplementaryProcessData supplementaryProcessData) throws JsonException {
        final List<JavaScript> javaScriptsBase64 = flow.getContent().getComponents().get(0).getContent().getJavascripts();
        final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>();
        for (JavaScript javascriptBase64 : javaScriptsBase64) {
            javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(), Base64Util.base64decode(javascriptBase64.getJavascript())));
        }
        final JSWrapperSingleScript scriptWrapper = new JSWrapperSingleScript(javaScripts);

        Object supProcDataJson = convertSupplementaryProcessDataToJsJsonObject(scriptWrapper, supplementaryProcessData);
        LOGGER.trace("Starting javascript with invocation method: [{}]", flow.getContent().getComponents().get(0).getContent().getInvocationMethod());
        final Object result = scriptWrapper.callMethod(flow.getContent().getComponents().get(0).getContent().getInvocationMethod(), new Object[]{record, supProcDataJson});
        return (String) result;
    }

    private Object convertSupplementaryProcessDataToJsJsonObject(JSWrapperSingleScript scriptWrapper, SupplementaryProcessData supplementaryProcessData) throws JsonException {
        // Something about why you need parantheses in the string around the json
        // when trying to evalute the json in javascript (rhino):
        // http://rayfd.me/2007/03/28/why-wont-eval-eval-my-json-or-json-object-object-literal/
        String jsonStr = "(" + JsonUtil.toJson(supplementaryProcessData) + ")"; // notice the parantheses!
        return scriptWrapper.eval(jsonStr);
    }
}
