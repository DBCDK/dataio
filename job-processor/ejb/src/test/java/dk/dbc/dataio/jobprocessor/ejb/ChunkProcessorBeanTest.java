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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
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
import static org.hamcrest.CoreMatchers.nullValue;
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
                .setItems(new ArrayList<>(0))
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
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
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
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics().size(), is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", processedItem1.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        final ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[2] diagnostic", processedItem2.getDiagnostics(), is(nullValue()));
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
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics().size(), is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", processedItem1.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
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
        assertThat("Chunk item[0] data", processedItem0.getData().length == 0, is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
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
        assertThat("Chunk item[0] data", processedItem0.getData().length == 0, is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
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
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()),
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
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
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
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
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

    @Test
    public void process_skipsOnlyChunkItemsWithStatusFailureAndIgnore_returnsResultOfJavascriptPipe() throws Exception {

        final String record = "zero";
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnConcatenation,
                getJavaScript(getJavaScriptReturnConcatenationFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);

        final List<ChunkItem> items = new ArrayList<>();
        items.add(new ChunkItemBuilder().setId(0).setData(StringUtil.asBytes(record)).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.IGNORE).build());

        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(items)
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = chunkProcessorBean.process(chunk, flow, supplementaryProcessData);
        assertProcessedChunk(processedChunk, jobId, 1, 3);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()),
                is(String.format("%s%s%s", submitter, record.toUpperCase(), format)));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] data", processedItem1.getData().length == 0, is(false));
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics(), is(nullValue()));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        final ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] data", processedItem2.getData().length == 0, is(false));
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[2] diagnostic", processedItem2.getDiagnostics(), is(nullValue()));

        assertThat("Chunk has item[3]", iterator.hasNext(), is(false));
    }

    /*
     * private methods
     */


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
                    .setData(StringUtil.asBytes(itemData))
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
                .setJavascript(StringUtil.base64encode(javascript))
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
