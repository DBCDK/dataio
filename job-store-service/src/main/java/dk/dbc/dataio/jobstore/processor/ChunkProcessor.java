package dk.dbc.dataio.jobstore.processor;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64decode;
import static dk.dbc.dataio.jobstore.util.Base64Util.base64encode;
import java.nio.charset.Charset;

public class ChunkProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProcessor.class);

    private ChunkProcessor() { }

    public static ChunkResult processChunk(Chunk chunk) {
        LOGGER.info("Processing chunk: {}", chunk.getId());
        final Flow flow = chunk.getFlow();

        final List<String> processedResults = new ArrayList<>();
        for (String record : chunk.getRecords()) {
            final String processedRecord = processRecord(flow, base64decode(record));
            final String processedRecordBase64 = base64encode(processedRecord);
            processedResults.add(processedRecordBase64);
        }
        // todo: Change below to get actual jobId, chunkId and Charset
        return new ChunkResult(chunk.getId(), chunk.getId(), Charset.defaultCharset(), processedResults);
    }

    private static String processRecord(Flow flow, String record) {
        LOGGER.trace("Record: {}", record);
        return javascriptRecordHandler(flow, record);
    }

    private static String javascriptRecordHandler(Flow flow, String record) {
        final List<JavaScript> javascriptsBase64 = flow.getContent().getComponents().get(0).getContent().getJavascripts();
        final List<StringSourceSchemeHandler.Script> javascripts = new ArrayList<>();
        for (JavaScript javascriptBase64 : javascriptsBase64) {
            javascripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(), base64decode(javascriptBase64.getJavascript())));
        }
        final JSWrapperSingleScript scriptWrapper = new JSWrapperSingleScript(javascripts);
        LOGGER.info("Starting javascript with invocation method: [{}]", flow.getContent().getComponents().get(0).getContent().getInvocationMethod());
        final Object res = scriptWrapper.callMethod(flow.getContent().getComponents().get(0).getContent().getInvocationMethod(), new Object[]{record});
        return (String)res;
    }
}
