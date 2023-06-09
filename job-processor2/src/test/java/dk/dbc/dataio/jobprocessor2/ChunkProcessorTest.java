package dk.dbc.dataio.jobprocessor2;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ChunkProcessorTest {
    static final String javaScriptReturnUpperCase = "returnUpperCase";
    static final String javaScriptReturnEmptyString = "returnEmptyString";
    static final String javaScriptReturnNoResult = "returnNoResult";
    static final String javaScriptReturnConcatenation = "returnConcatenation";
    static final String javaScriptThrowException = "throwException";
    static final String javaScriptThrowIllegalOperationOnControlFieldException = "throwIOOCFException";
    static final String trackingId = "trackingId_";
    private final int jobId = 42;
    private final long submitter = 123;
    private final String format = "DasFormat";
    private final String additionalArgs = String.format("{\"format\":\"%s\",\"submitter\":%s}", format, submitter);

    public static FlowComponentContent getFlowComponentContent(ScriptWrapper scriptWrapper) {
        List<JavaScript> js = List.of(scriptWrapper.javaScript,
                new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorTest.class, "javascript/jscommon/system/Use.use.js"), "Use"),
                new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorTest.class, "javascript/jscommon/system/ModulesInfo.use.js"), "ModulesInfo"),
                new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorTest.class, "javascript/jscommon/system/Use.RequiredModules.use.js"), "Use.RequiredModules"),
                new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorTest.class, "javascript/jscommon/external/ES5.use.js"), "ES5"),
                new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorTest.class, "javascript/jscommon/system/Engine.use.js"), "Engine")
        );
        return new FlowComponentContentBuilder().setInvocationMethod(scriptWrapper.invocationMethod).setJavascripts(js).build();
    }

    public static JavaScript getJavaScript(String javascript) {
        return new JavaScriptBuilder().setJavascript(StringUtil.base64encode(javascript)).build();
    }

    public static String getJavaScriptReturnUpperCaseFunction() {
        return "" + "function " + javaScriptReturnUpperCase + "(str) {\n" + "    return str.toUpperCase();\n" + "}\n";
    }

    public static String getJavaScriptReturnEmptyStringFunction() {
        return "" + "function " + javaScriptReturnEmptyString + "(str) {\n" + "    return \"\";\n" + "}\n";
    }

    public static String getJavaScriptReturnNoResultFunction() {
        return "" + "function " + javaScriptReturnNoResult + "(str) {\n" + "    return;\n" + "}\n";
    }

    public static String getJavaScriptThrowExceptionFunction() {
        return "" + "function " + javaScriptThrowException + "(str) {\n" + "    if(str === \"throw\" || str === \"\") {\n" + "      throw \"this is an exception from JavaScript\";\n" + "    } else {\n" + "      return str;\n" + "    }\n" + "}\n";
    }

    public static String getJavaScriptThrowIllegalOperationOnControlFieldExceptionFunction() {
        return "" + "function " + javaScriptThrowIllegalOperationOnControlFieldException + "(str) {\n" + "    throw \"Illegal operation on control field\";\n" + "}\n";
    }

    public static String getJavaScriptReturnConcatenationFunction() {
        return "" + "function " + javaScriptReturnConcatenation + "(str, processData) {\n" + "    return processData.submitter + str + processData.format;\n" + "}\n";
    }

    @Test
    public void emptyChunk_returnsEmptyResult() {
        Chunk emptyChunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(new ArrayList<>(0)).build();
        ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        Flow flow = getFlow(scriptWrapper);

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(emptyChunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, emptyChunk.getChunkId(), 0);
    }

    private ChunkProcessor makeChunkProcessor() {
        return new ChunkProcessor(Mockito.mock(HealthService.class));
    }

    @Test
    public void exceptionThrownFromJavascript_chunkItemFailure() {
        ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException, getJavaScript(getJavaScriptThrowExceptionFunction()));
        Flow flow = getFlow(scriptWrapper);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("throw")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostics", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic level", processedItem0.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
    }

    @Test
    public void illegalOperationOnControlFieldExceptionThrownFromJavascript_chunkItemFailure() {
        ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowIllegalOperationOnControlFieldException, getJavaScript(getJavaScriptThrowIllegalOperationOnControlFieldExceptionFunction()));
        Flow flow = getFlow(scriptWrapper);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("throw")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostics", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic level", processedItem0.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.ERROR));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
    }

    @Test
    public void exceptionThrownFromOneOutOfThreeJavascripts_chunkItemFailureForThrowSuccessForRest() {
        ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException, getJavaScript(getJavaScriptThrowExceptionFunction()));
        Flow flow = getFlow(scriptWrapper);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("zero", "throw", "two")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 3);
        Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics().size(), is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", processedItem1.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[1] trackingId", processedItem1.getTrackingId(), is(trackingId + 2));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[2] diagnostic", processedItem2.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[2] trackingId", processedItem2.getTrackingId(), is(trackingId + 3));
    }

    @Test
    public void illegalJavascriptInEnvironment_chunkItemFailure() {
        ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptThrowException, getJavaScript("This is not a legal javascript!"));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("zero", "one")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 2);
        Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics().size(), is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", processedItem1.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[1] trackingId", processedItem1.getTrackingId(), is(trackingId + 2));
    }

    @Test
    public void javascriptReturnsEmptyString_chunkItemIgnored() {
        ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnEmptyString, getJavaScript(getJavaScriptReturnEmptyStringFunction()));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("zero")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", processedItem0.getData().length == 0, is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Ignore("Testing better handling of ClassCastException")
    @Test
    public void javascriptReturnsWithNoResult_chunkItemFailed() {
        ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult, getJavaScript(getJavaScriptReturnNoResultFunction()));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("zero")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", processedItem0.getData().length == 0, is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void multipleFlowComponents_returnsResultOfJavascriptPipe() {
        final String record = "zero";
        ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnConcatenation, getJavaScript(getJavaScriptReturnConcatenationFunction()));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems(record)).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is(String.format("%s%s%s", submitter, record.toUpperCase(), format)));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void javaScriptIgnoreRecord() {
        ScriptWrapper scriptWrapper1 = new ScriptWrapper("throwIgnore", getJavaScript("function throwIgnore() {" + "Packages.dk.dbc.javascript.recordprocessing.IgnoreRecord.doThrow('errorMessage');" + "}"));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult, getJavaScript(getJavaScriptReturnNoResultFunction()));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("zero")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void javaScriptFailRecord() {
        ScriptWrapper scriptWrapper1 = new ScriptWrapper("throwIgnore", getJavaScript("function throwIgnore() {" + "Packages.dk.dbc.javascript.recordprocessing.FailRecord.doThrow('errorMessage');" + "}"));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult, getJavaScript(getJavaScriptReturnNoResultFunction()));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("zero")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void flowHasNextComponents_returnsChunkWithNextItems() {
        FlowComponent flowComponent = new FlowComponentBuilder().setContent(getFlowComponentContent(new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction())))).setNext(getFlowComponentContent(new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction())))).build();
        Flow flow = new FlowBuilder().setContent(new FlowContentBuilder().setComponents(Collections.singletonList(flowComponent)).build()).build();
        final String record = "zero";
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems(record)).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertThat("Chunk has next items", processedChunk.hasNextItems(), is(true));
    }

    @Test
    public void skipsOnlyChunkItemsWithStatusFailureAndIgnore_returnsResultOfJavascriptPipe() {

        final String record = "zero";
        ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnConcatenation, getJavaScript(getJavaScriptReturnConcatenationFunction()));
        Flow flow = getFlow(scriptWrapper1, scriptWrapper2);

        List<ChunkItem> items = new ArrayList<>();
        items.add(new ChunkItemBuilder().setId(0).setData(StringUtil.asBytes(record)).setTrackingId(trackingId + 1).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.FAILURE).setTrackingId(trackingId + 2).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.IGNORE).setTrackingId(trackingId + 3).build());

        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(items).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk processedChunk = chunkProcessor.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, 1, 3);
        Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is(String.format("%s%s%s", submitter, record.toUpperCase(), format)));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] data", processedItem1.getData().length == 0, is(false));
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[1] trackingId", processedItem1.getTrackingId(), is(trackingId + 2));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] data", processedItem2.getData().length == 0, is(false));
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[2] diagnostic", processedItem2.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[2] trackingId", processedItem2.getTrackingId(), is(trackingId + 3));

        assertThat("Chunk has item[3]", iterator.hasNext(), is(false));
    }

    @Test
    public void chunkItemIsOfTypeAddi_scriptArgumentsAreReadFromAddiContentAndAddiMetadata() throws Exception {
        AddiMetaData addiMetaData = new AddiMetaData().withSubmitterNumber(654321).withFormat("addiContentFormat");
        AddiRecord addiRecord = new AddiRecord(StringUtil.asBytes(new JSONBContext().marshall(addiMetaData)), StringUtil.asBytes("addiContent"));

        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setItems(Collections.singletonList(ChunkItem.successfulChunkItem().withId(0).withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES).withData(addiRecord.getBytes()))).build();

        ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptReturnConcatenation, getJavaScript(getJavaScriptReturnConcatenationFunction()));
        Flow flow = getFlow(scriptWrapper);

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        Chunk result = chunkProcessor.process(chunk, flow, additionalArgs);

        Iterator<ChunkItem> iterator = result.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem resultItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(resultItem0.getData()), is(String.format("%s%s%s", addiMetaData.submitterNumber(), "addiContent", addiMetaData.format())));
        assertThat("Chunk item[0] status", resultItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void flowNotInCache() {
        ChunkProcessor chunkProcessor = makeChunkProcessor();
        assertThat(chunkProcessor.getCachedFlow(42, 1), is(Optional.empty()));
    }

    @Test
    public void flowInCache() {
        ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException, getJavaScript(getJavaScriptThrowExceptionFunction()));
        Flow flow = getFlow(scriptWrapper);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(jobId).setItems(getItems("throw")).build();

        ChunkProcessor chunkProcessor = makeChunkProcessor();
        chunkProcessor.process(chunk, flow, null);
        assertThat(chunkProcessor.getCachedFlow(flow.getId(), flow.getVersion()).isPresent(), is(true));
    }

    private void assertProcessedChunk(Chunk chunk, int jobID, long chunkId, int chunkSize) {
        assertThat("Chunk", chunk, is(notNullValue()));
        assertThat("Chunk job ID", chunk.getJobId(), is(jobID));
        assertThat("Chunk ID", chunk.getChunkId(), is(chunkId));
        assertThat("Chunk size", chunk.size(), is(chunkSize));
        assertThat("Chunk type", chunk.getType(), is(Chunk.Type.PROCESSED));
    }

    private List<ChunkItem> getItems(String... data) {
        List<ChunkItem> items = new ArrayList<>(data.length);
        int chunkId = 0;
        for (String itemData : data) {
            items.add(new ChunkItemBuilder().setId(chunkId++).setData(StringUtil.asBytes(itemData)).setTrackingId(trackingId + chunkId).build());
        }
        return items;
    }

    Flow getFlow(ScriptWrapper... scriptWrappers) {
        return new FlowBuilder().setContent(getFlowContent(scriptWrappers)).build();
    }

    private FlowContent getFlowContent(ScriptWrapper... scriptWrappers) {
        List<FlowComponent> flowComponents = new ArrayList<>(scriptWrappers.length);
        for (ScriptWrapper scriptWrapper : scriptWrappers) {
            flowComponents.add(getFlowComponent(scriptWrapper));
        }
        return new FlowContentBuilder().setComponents(flowComponents).build();
    }

    private FlowComponent getFlowComponent(ScriptWrapper scriptWrapper) {
        return new FlowComponentBuilder().setContent(getFlowComponentContent(scriptWrapper)).build();
    }

    public static class ScriptWrapper {
        public final String invocationMethod;
        public final JavaScript javaScript;

        public ScriptWrapper(String invocationMethod, JavaScript javaScript) {
            this.invocationMethod = invocationMethod;
            this.javaScript = javaScript;
        }
    }
}
