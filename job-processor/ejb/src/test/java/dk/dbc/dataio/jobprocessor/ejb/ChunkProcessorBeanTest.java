package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ChunkProcessorBeanTest {
    private final long jobId = 42;
    private final long submitter = 123;
    private final String format = "DasFormat";
    static final String javaScriptReturnUpperCase = "returnUpperCase";
    static final String javaScriptReturnEmptyString = "returnEmptyString";
    static final String javaScriptReturnNoResult = "returnNoResult";
    static final String javaScriptReturnConcatenation = "returnConcatenation";
    static final String javaScriptThrowException = "throwException";

    private final SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessData(submitter, format);

    @Test
    public void process_emptyChunk_returnsEmptyResult() throws Exception {
        final ExternalChunk emptyChunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(new ArrayList<ChunkItem>(0))
                .build();
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final Flow flow = getFlow(scriptWrapper);

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(emptyChunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, emptyChunk.getChunkId(), 0);
    }

    @Test
    public void process_exceptionThrownFromJavascript_chunkItemFailure() throws Exception {
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException,
                getJavaScript(getJavaScriptThrowExceptionFunction()));
        final Flow flow = getFlow(scriptWrapper);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("throw"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void process_exceptionThrownFromOneOutOfThreeJavascripts_chunkItemFailureForThrowSuccessForRest() throws Exception {
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException,
                getJavaScript(getJavaScriptThrowExceptionFunction()));
        final Flow flow = getFlow(scriptWrapper);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero", "throw", "two"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 3);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        final ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void process_illegalJavascriptInEnvironment_chunkItemFailure() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptThrowException,
                getJavaScript("This is not a legal javascript!"));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero", "one"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 2);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
    }
    
    @Test
    public void process_javascriptReturnsEmptyString_chunkItemIgnored() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnEmptyString,
                getJavaScript(getJavaScriptReturnEmptyStringFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();
        
        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", processedItem0.getData().isEmpty(), is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void process_javascriptReturnsWithNoResult_chunkItemFailed() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult,
                getJavaScript(getJavaScriptReturnNoResultFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();
        
        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", processedItem0.getData().isEmpty(), is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void process_multipleFlowComponents_returnsResultOfJavascriptPipe() throws Exception {
        final String record = "zero";
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnConcatenation,
                getJavaScript(getJavaScriptReturnConcatenationFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems(record))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", Base64Util.base64decode(processedItem0.getData()),
                is(String.format("%s%s%s", submitter, record.toUpperCase(), format)));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void process_javaScriptIgnoreRecord() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper("throwIgnore",
                getJavaScript("function throwIgnore() {" +
                        "Packages.dk.dbc.javascript.recordprocessing.IgnoreRecord.doThrow('errorMessage');" +
                        "}"));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult,
                getJavaScript(getJavaScriptReturnNoResultFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", Base64Util.base64decode(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void process_javaScriptFailRecord() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper("throwIgnore",
                getJavaScript("function throwIgnore() {" +
                        "Packages.dk.dbc.javascript.recordprocessing.FailRecord.doThrow('errorMessage');" +
                        "}"));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult,
                getJavaScript(getJavaScriptReturnNoResultFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", Base64Util.base64decode(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void process_flowHasNextComponents_returnsChunkWithNextItems() throws Exception {
        final FlowComponent flowComponent = new FlowComponentBuilder()
                .setContent(getFlowComponentContent(
                        new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()))))
                .setNext(getFlowComponentContent(
                        new ScriptWrapper(javaScriptReturnUpperCase, getJavaScript(getJavaScriptReturnUpperCaseFunction()))))
                .build();
        final Flow flow = new FlowBuilder()
                .setContent(new FlowContentBuilder()
                        .setComponents(Collections.singletonList(flowComponent))
                        .build())
                .build();
        final String record = "zero";
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems(record))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertThat("Chunk has next items", processedChunk.hasNextItems(), is(true));
    }


    private void assertProcessedChunk(ExternalChunk chunk, long jobID, long chunkId, int chunkSize) {
        assertThat("Chunk", chunk, is(notNullValue()));
        assertThat("Chunk job ID", chunk.getJobId(), is(jobID));
        assertThat("Chunk ID", chunk.getChunkId(), is(chunkId));
        assertThat("Chunk size", chunk.size(), is(chunkSize));
        assertThat("Chunk type", chunk.getType(), is(ExternalChunk.Type.PROCESSED));
    }

    private List<ChunkItem> getItems(String... data) {
        final List<ChunkItem> items = new ArrayList<>(data.length);
        int chunkId = 0;
        for (String itemData : data) {
            items.add(new ChunkItemBuilder()
                    .setId(chunkId++)
                    .setData(Base64Util.base64encode(itemData))
                    .build());
        }
        return items;
    }

    private ChunkProcessorBean getInitializedBean() {
        return new ChunkProcessorBean();
    }

    Flow getFlow(ScriptWrapper... scriptWrappers) throws Exception {
        return new FlowBuilder()
                .setContent(getFlowContent(scriptWrappers))
                .build();
    }

    private FlowContent getFlowContent(ScriptWrapper... scriptWrappers) throws Exception {
        List<FlowComponent> flowComponents = new ArrayList<>(scriptWrappers.length);
        for (ScriptWrapper scriptWrapper : scriptWrappers) {
            flowComponents.add(getFlowComponent(scriptWrapper));
        }
        return new FlowContentBuilder()
                .setComponents(flowComponents)
                .build();
    }

    private FlowComponent getFlowComponent(ScriptWrapper scriptWrapper) throws Exception {
        return new FlowComponentBuilder()
                .setContent(getFlowComponentContent(scriptWrapper))
                .build();
    }

    public static FlowComponentContent getFlowComponentContent(ScriptWrapper scriptWrapper) throws Exception {
        final String modulesInfoModuleResource = "/ModulesInfo.json";
        final String useModuleResource = "/Use.json";
        final JSONBContext jsonbContext = new JSONBContext();
        return new FlowComponentContentBuilder()
                .setInvocationMethod(scriptWrapper.invocationMethod)
                .setJavascripts(Arrays.asList(
                        scriptWrapper.javaScript,
                        jsonbContext.unmarshall(resourceToString(modulesInfoModuleResource), JavaScript.class),
                        jsonbContext.unmarshall(resourceToString(useModuleResource), JavaScript.class)))
                .build();
    }

    public static JavaScript getJavaScript(String javascript) {
        return new JavaScriptBuilder()
                .setJavascript(Base64Util.base64encode(javascript))
                .build();
    }

    public static String getJavaScriptReturnUpperCaseFunction() {
        return ""
                + "function " + javaScriptReturnUpperCase + "(str) {\n"
                + "    return str.toUpperCase();\n"
                + "}\n";
    }

    public static String getJavaScriptReturnEmptyStringFunction() {
        return ""
                + "function " + javaScriptReturnEmptyString + "(str) {\n"
                + "    return \"\";\n"
                + "}\n";
    }

    public static String getJavaScriptReturnNoResultFunction() {
        return ""
                + "function " + javaScriptReturnNoResult + "(str) {\n"
                + "    return;\n"
                + "}\n";
    }

    public static String getJavaScriptThrowExceptionFunction() {
        return ""
                + "function " + javaScriptThrowException + "(str) {\n"
                + "    if(str === \"throw\" || str === \"\") {\n"
                + "      throw \"this is an exception from JavaScript\";\n"
                + "    } else {\n"
                + "      return str;\n"
                + "    }\n"
                + "}\n";
    }

    public static String getJavaScriptReturnConcatenationFunction() {
        return ""
                + "function " + javaScriptReturnConcatenation + "(str, processData) {\n"
                + "    return processData.submitter + str + processData.format;\n"
                + "}\n";
    }

    public static String resourceToString(String resourceName) throws Exception {
        final java.net.URL url = ChunkProcessorBeanTest.class.getResource(resourceName);
        final java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
        return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
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
