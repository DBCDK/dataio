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

import dk.dbc.commons.addi.AddiRecord;
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
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ChunkProcessorBeanTest {
    private final long jobId = 42;
    private final long submitter = 123;
    private final String format = "DasFormat";
    static final String javaScriptReturnUpperCase = "returnUpperCase";
    static final String javaScriptReturnEmptyString = "returnEmptyString";
    static final String javaScriptReturnNoResult = "returnNoResult";
    static final String javaScriptReturnConcatenation = "returnConcatenation";
    static final String javaScriptThrowException = "throwException";
    static final String javaScriptThrowIllegalOperationOnControlFieldException = "throwIOOCFException";
    static final String trackingId = "trackingId_";

    private final String additionalArgs = String.format("{\"format\":\"%s\",\"submitter\":%s}", format, submitter);

    @Test
    public void emptyChunk_returnsEmptyResult() throws Exception {
        final Chunk emptyChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(new ArrayList<>(0))
                .build();
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final Flow flow = getFlow(scriptWrapper);

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(emptyChunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, emptyChunk.getChunkId(), 0);
    }

    @Test
    public void exceptionThrownFromJavascript_chunkItemFailure() throws Exception {
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException,
                getJavaScript(getJavaScriptThrowExceptionFunction()));
        final Flow flow = getFlow(scriptWrapper);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("throw"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostics", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic level", processedItem0.getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.FATAL));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(),
                is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(),
                is(trackingId + 1));
    }

    @Test
    public void illegalOperationOnControlFieldExceptionThrownFromJavascript_chunkItemFailure() throws Exception {
        final ScriptWrapper scriptWrapper = new ScriptWrapper(
                javaScriptThrowIllegalOperationOnControlFieldException,
                getJavaScript(getJavaScriptThrowIllegalOperationOnControlFieldExceptionFunction()));
        final Flow flow = getFlow(scriptWrapper);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("throw"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostics", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic level", processedItem0.getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.ERROR));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(),
                is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(),
                is(trackingId + 1));
    }

    @Test
    public void exceptionThrownFromOneOutOfThreeJavascripts_chunkItemFailureForThrowSuccessForRest() throws Exception {
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException,
                getJavaScript(getJavaScriptThrowExceptionFunction()));
        final Flow flow = getFlow(scriptWrapper);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero", "throw", "two"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 3);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics().size(), is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", processedItem1.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[1] trackingId", processedItem1.getTrackingId(), is(trackingId + 2));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        final ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[2] diagnostic", processedItem2.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[2] trackingId", processedItem2.getTrackingId(), is(trackingId + 3));
    }

    @Test
    public void illegalJavascriptInEnvironment_chunkItemFailure() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptThrowException,
                getJavaScript("This is not a legal javascript!"));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero", "one"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 2);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics().size(), is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", processedItem1.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[1] trackingId", processedItem1.getTrackingId(), is(trackingId + 2));
    }
    
    @Test
    public void javascriptReturnsEmptyString_chunkItemIgnored() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnEmptyString,
                getJavaScript(getJavaScriptReturnEmptyStringFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();
        
        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", processedItem0.getData().length == 0, is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Ignore("Testing better handling of ClassCastException")
    @Test
    public void javascriptReturnsWithNoResult_chunkItemFailed() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult,
                getJavaScript(getJavaScriptReturnNoResultFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();
        
        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", processedItem0.getData().length == 0, is(false));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void multipleFlowComponents_returnsResultOfJavascriptPipe() throws Exception {
        final String record = "zero";
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnConcatenation,
                getJavaScript(getJavaScriptReturnConcatenationFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems(record))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()),
                is(String.format("%s%s%s", submitter, record.toUpperCase(), format)));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void javaScriptIgnoreRecord() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper("throwIgnore",
                getJavaScript("function throwIgnore() {" +
                        "Packages.dk.dbc.javascript.recordprocessing.IgnoreRecord.doThrow('errorMessage');" +
                        "}"));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult,
                getJavaScript(getJavaScriptReturnNoResultFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void javaScriptFailRecord() throws Exception {
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper("throwIgnore",
                getJavaScript("function throwIgnore() {" +
                        "Packages.dk.dbc.javascript.recordprocessing.FailRecord.doThrow('errorMessage');" +
                        "}"));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnNoResult,
                getJavaScript(getJavaScriptReturnNoResultFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("zero"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, chunk.getChunkId(), 1);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()), is("errorMessage"));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics().size(), is(1));
        assertThat("Chunk item[0] diagnostic stacktrace", processedItem0.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));
        assertThat("Chunk has item[1]", iterator.hasNext(), is(false));
    }

    @Test
    public void flowHasNextComponents_returnsChunkWithNextItems() throws Exception {
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
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems(record))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertThat("Chunk has next items", processedChunk.hasNextItems(), is(true));
    }

    @Test
    public void skipsOnlyChunkItemsWithStatusFailureAndIgnore_returnsResultOfJavascriptPipe() throws Exception {

        final String record = "zero";
        final ScriptWrapper scriptWrapper1 = new ScriptWrapper(javaScriptReturnUpperCase,
                getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final ScriptWrapper scriptWrapper2 = new ScriptWrapper(javaScriptReturnConcatenation,
                getJavaScript(getJavaScriptReturnConcatenationFunction()));
        final Flow flow = getFlow(scriptWrapper1, scriptWrapper2);

        final List<ChunkItem> items = new ArrayList<>();
        items.add(new ChunkItemBuilder().setId(0).setData(StringUtil.asBytes(record)).setTrackingId(trackingId + 1).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.FAILURE).setTrackingId(trackingId + 2).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.IGNORE).setTrackingId(trackingId + 3).build());

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(items)
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk processedChunk = chunkProcessorBean.process(chunk, flow, additionalArgs);
        assertProcessedChunk(processedChunk, jobId, 1, 3);
        final Iterator<ChunkItem> iterator = processedChunk.iterator();

        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem processedItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(processedItem0.getData()),
                is(String.format("%s%s%s", submitter, record.toUpperCase(), format)));
        assertThat("Chunk item[0] status", processedItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] diagnostic", processedItem0.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[0] trackingId", processedItem0.getTrackingId(), is(trackingId + 1));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        final ChunkItem processedItem1 = iterator.next();
        assertThat("Chunk item[1] data", processedItem1.getData().length == 0, is(false));
        assertThat("Chunk item[1] status", processedItem1.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[1] diagnostic", processedItem1.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[1] trackingId", processedItem1.getTrackingId(), is(trackingId + 2));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        final ChunkItem processedItem2 = iterator.next();
        assertThat("Chunk item[2] data", processedItem2.getData().length == 0, is(false));
        assertThat("Chunk item[2] status", processedItem2.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("Chunk item[2] diagnostic", processedItem2.getDiagnostics(), is(nullValue()));
        assertThat("Chunk item[2] trackingId", processedItem2.getTrackingId(), is(trackingId + 3));

        assertThat("Chunk has item[3]", iterator.hasNext(), is(false));
    }

    @Test
    public void chunkItemIsOfTypeAddi_scriptArgumentsAreReadFromAddiContentAndAddiMetadata() throws Exception {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber(654321)
                .withFormat("addiContentFormat");
        final AddiRecord addiRecord = new AddiRecord(
                StringUtil.asBytes(new JSONBContext().marshall(addiMetaData)),
                StringUtil.asBytes("addiContent"));

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setItems(Collections.singletonList(
                        ChunkItem.successfulChunkItem()
                            .withId(0)
                            .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                            .withData(addiRecord.getBytes())))
                .build();

        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptReturnConcatenation,
                getJavaScript(getJavaScriptReturnConcatenationFunction()));
        final Flow flow = getFlow(scriptWrapper);

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final Chunk result = chunkProcessorBean.process(chunk, flow, additionalArgs);

        final Iterator<ChunkItem> iterator = result.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        final ChunkItem resultItem0 = iterator.next();
        assertThat("Chunk item[0] data", StringUtil.asString(resultItem0.getData()),
                is(String.format("%s%s%s", addiMetaData.submitterNumber(), "addiContent", addiMetaData.format())));
        assertThat("Chunk item[0] status", resultItem0.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void flowNotInCache() {
        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        assertThat(chunkProcessorBean.getCachedFlow(42, 1), is(Optional.empty()));
    }

    @Test
    public void flowInCache() throws Exception {
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptThrowException,
                getJavaScript(getJavaScriptThrowExceptionFunction()));
        final Flow flow = getFlow(scriptWrapper);
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(getItems("throw"))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        chunkProcessorBean.process(chunk, flow, null);
        assertThat(chunkProcessorBean.getCachedFlow(flow.getId(), flow.getVersion()).isPresent(), is(true));
    }

    @Test
    public void encountersClassCastException() throws Throwable {
        final ChunkProcessorBean chunkProcessorBean = spy(new ChunkProcessorBean());
        chunkProcessorBean.healthBean = new HealthBean();
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
            .setJobId(jobId)
            .setItems(getItems("item1", "item2"))
            .build();
        final ScriptWrapper scriptWrapper = new ScriptWrapper(javaScriptReturnUpperCase,
            getJavaScript(getJavaScriptReturnUpperCaseFunction()));
        final Flow flow = getFlow(scriptWrapper);
        ClassCastException classCastException = new ClassCastException(
            "java.lang.invoke.LambdaForm cannot be cast to " +
            "[Ljava.lang.invoke.LambdaFormEditor$Transform;");
        when(chunkProcessorBean.cacheFlow(flow)).thenThrow(classCastException);

        // try block instead of Test(expected =) to ensure that only the
        // specific exception is caught and only when thrown by the method
        try {
            chunkProcessorBean.process(chunk, flow, additionalArgs);
            fail("did not encounter the expected exception");
        } catch(RuntimeException e) {
            assertThat("terminallyIll flag true",
                chunkProcessorBean.healthBean.isTerminallyIll(), is(true));
            assertThat("check exception cause",
                chunkProcessorBean.healthBean.getCause(), is(classCastException));
            assertThat("exception is thrown", e.getMessage(),
                is("Processor reported itself terminally ill (bug 20964)"));
        }
    }

    private void assertProcessedChunk(Chunk chunk, long jobID, long chunkId, int chunkSize) {
        assertThat("Chunk", chunk, is(notNullValue()));
        assertThat("Chunk job ID", chunk.getJobId(), is(jobID));
        assertThat("Chunk ID", chunk.getChunkId(), is(chunkId));
        assertThat("Chunk size", chunk.size(), is(chunkSize));
        assertThat("Chunk type", chunk.getType(), is(Chunk.Type.PROCESSED));
    }

    private List<ChunkItem> getItems(String... data) {
        final List<ChunkItem> items = new ArrayList<>(data.length);
        int chunkId = 0;
        for (String itemData : data) {
            items.add(new ChunkItemBuilder()
                    .setId(chunkId++)
                    .setData(StringUtil.asBytes(itemData))
                    .setTrackingId(trackingId + chunkId)
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
        return new FlowComponentContentBuilder()
                .setInvocationMethod(scriptWrapper.invocationMethod)
                .setJavascripts(Arrays.asList(
                        scriptWrapper.javaScript,
                        new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorBeanTest.class, "javascript/jscommon/system/Use.use.js"), "Use"),
                        new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorBeanTest.class, "javascript/jscommon/system/ModulesInfo.use.js"), "ModulesInfo"),
                        new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorBeanTest.class, "javascript/jscommon/system/Use.RequiredModules.use.js"), "Use.RequiredModules"),
                        new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorBeanTest.class, "javascript/jscommon/external/ES5.use.js"), "ES5"),
                        new JavaScript(ResourceReader.getResourceAsBase64(ChunkProcessorBeanTest.class, "javascript/jscommon/system/Engine.use.js"), "Engine")))
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

    public static String getJavaScriptThrowIllegalOperationOnControlFieldExceptionFunction() {
        return ""
                + "function " + javaScriptThrowIllegalOperationOnControlFieldException + "(str) {\n"
                + "    throw \"Illegal operation on control field\";\n"
                + "}\n";
    }

    public static String getJavaScriptReturnConcatenationFunction() {
        return ""
                + "function " + javaScriptReturnConcatenation + "(str, processData) {\n"
                + "    return processData.submitter + str + processData.format;\n"
                + "}\n";
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
