package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.jobprocessor.javascript.JSWrapperSingleScript;
import dk.dbc.dataio.jobprocessor.javascript.StringSourceSchemeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64decode;
import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64encode;

/**
 * This Enterprise Java Bean (EJB) processes chunks with JavaScript contained in associated flow
 */
@Stateless
@LocalBean
public class ChunkProcessorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessorBean.class);

    /**
     * Processes given chunk
     *
     * @param jobId Id of job containing chunk
     * @param chunk chunk
     *
     * @return result of processing
     */
    public ChunkResult process(long jobId, Chunk chunk) {
        LOGGER.info("Processing chunk {} in job {}", chunk.getId(), jobId);
        final Flow flow = chunk.getFlow();
        final List<String> results = new ArrayList<>();
        for (String record : chunk.getRecords()) {
            final String processedRecord = invokeJavaScript(flow, base64decode(record));
            final String processedRecordBase64 = base64encode(processedRecord);
            results.add(processedRecordBase64);
        }
        // todo: Change Chunk to get actual Charset
        return new ChunkResult(chunk.getId(), chunk.getId(), Charset.defaultCharset(), results);
    }

    private String invokeJavaScript(Flow flow, String record) {
        final List<JavaScript> javaScriptsBase64 = flow.getContent().getComponents().get(0).getContent().getJavascripts();
        final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>();
        for (JavaScript javascriptBase64 : javaScriptsBase64) {
            javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(), base64decode(javascriptBase64.getJavascript())));
        }
        final JSWrapperSingleScript scriptWrapper = new JSWrapperSingleScript(javaScripts);
        LOGGER.trace("Starting javascript with invocation method: [{}]", flow.getContent().getComponents().get(0).getContent().getInvocationMethod());
        final Object result = scriptWrapper.callMethod(flow.getContent().getComponents().get(0).getContent().getInvocationMethod(), new Object[]{record});
        return (String)result;
    }
}
