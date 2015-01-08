package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChunkProcessorBeanTest {

    Logger LOGGER = LoggerFactory.getLogger(ChunkProcessorBeanTest.class);

    private final long jobId = 42;
    private final String javaScriptUppercaseInvocationMethod = "toUpper";
    private final String javaScriptThrowExceptionInvocationMethod = "throwException";
    private final String javaScriptConcatenateInvocationMethod = "doConcatenation";
    private final String modulesInfoModuleResource = "/ModulesInfo.json";
    private final String useModuleResource = "/Use.json";

    private final SupplementaryProcessData testSuppData = new SupplementaryProcessData(424242L, "latin-1");

    @Test
    public void process_emptyChunk_returnsEmptyResult() throws Exception {
        final ExternalChunk emptyChunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(new ArrayList<ChunkItem>(0))
                .build();
        final Flow flow = getFlow(javaScriptUppercaseInvocationMethod, getJavaScript(getJavaScriptToUpperFunction()));

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(emptyChunk, flow, testSuppData);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(emptyChunk.getChunkId()));
        assertThat(chunkResult.getItems().size(), is(0));
    }

    @Test
    public void process_chunkWithData_returnsResultOfJavaScriptProcessing() throws Exception {
        final String record1 = "one";
        final ChunkItem item1 = new ChunkItemBuilder()
                .setId(0)
                .setData(Base64Util.base64encode(record1))
                .build();
        final String record2 = "two";
        final ChunkItem item2 = new ChunkItemBuilder()
                .setId(1)
                .setData(Base64Util.base64encode(record2))
                .build();
        final Flow flow = getFlow(javaScriptUppercaseInvocationMethod, getJavaScript(getJavaScriptToUpperFunction()));
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setJobId(jobId)
                .setItems(Arrays.asList(item1, item2))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk, flow, testSuppData);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getItems().size(), is(2));
        assertThat(Base64Util.base64decode(chunkResult.getItems().get(0).getData()), is(record1.toUpperCase()));
        assertThat(Base64Util.base64decode(chunkResult.getItems().get(1).getData()), is(record2.toUpperCase()));
    }

    @Test
    public void process_chunkWithDataAndProcessData_returnsResultOfJavaScriptProcessing() throws Exception {
        final String record1 = "one";
        final ChunkItem item1 = new ChunkItemBuilder()
                .setId(0)
                .setData(Base64Util.base64encode(record1))
                .build();
        final String record2 = "two";
        final ChunkItem item2 = new ChunkItemBuilder()
                .setId(1)
                .setData(Base64Util.base64encode(record2))
                .build();
        final long submitter = 456456L;
        final String format = "DasFormat";
        final Flow flow = getFlow(javaScriptConcatenateInvocationMethod, getJavaScript(getJavaScriptConcatenateProcessDataFunction()));
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(Arrays.asList(item1, item2))
                .build();
        final SupplementaryProcessData suppData = new SupplementaryProcessData(submitter, format);

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk, flow, suppData);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getItems().size(), is(2));
        assertThat(Base64Util.base64decode(chunkResult.getItems().get(0).getData()), is("456456oneDasFormat"));
        assertThat(Base64Util.base64decode(chunkResult.getItems().get(1).getData()), is("456456twoDasFormat"));
    }

    @Test
    public void process_exceptionThrownFromJavascript_chunkItemFailure() throws Exception {
        final ChunkItem item1 = new ChunkItemBuilder()
                .setId(0)
                .setData(Base64Util.base64encode("throw"))
                .build();
        final Flow flow = getFlow(javaScriptThrowExceptionInvocationMethod, getJavaScript(getJavaScriptWhichThrowsException()));
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(Arrays.asList(item1))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk, flow, testSuppData);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getItems().size(), is(1));
        assertThat(chunkResult.getItems().get(0).getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void process_exceptionThrownFromOneOutOfThreeJavascripts_chunkItemFailureForThrowSuccessForRest() throws Exception {
        final ChunkItem item0 = new ChunkItemBuilder()
                .setId(0)
                .setData(Base64Util.base64encode("zero"))
                .build();
        final ChunkItem item1 = new ChunkItemBuilder()
                .setId(1)
                .setData(Base64Util.base64encode("throw"))
                .build();
        final ChunkItem item2 = new ChunkItemBuilder()
                .setId(2)
                .setData(Base64Util.base64encode("two"))
                .build();
        final Flow flow = getFlow(javaScriptThrowExceptionInvocationMethod, getJavaScript(getJavaScriptWhichThrowsException()));
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(Arrays.asList(item0, item1, item2))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk, flow, testSuppData);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getItems().size(), is(3));
        assertThat(chunkResult.getItems().get(0).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(chunkResult.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(chunkResult.getItems().get(2).getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void process_IllegalJavascriptInEnvironment_chunkItemFailure() throws Exception {
        final ChunkItem item0 = new ChunkItemBuilder()
                .setId(0)
                .setData(Base64Util.base64encode("zero"))
                .build();
        final ChunkItem item1 = new ChunkItemBuilder()
                .setId(1)
                .setData(Base64Util.base64encode("two"))
                .build();
        final Flow flow = getFlow(javaScriptThrowExceptionInvocationMethod, getJavaScript("This is not a legal javascript!"));
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED)
                .setJobId(jobId)
                .setItems(Arrays.asList(item0, item1))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk, flow, testSuppData);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getItems().size(), is(2));
        assertThat(chunkResult.getItems().get(0).getStatus(), is(ChunkItem.Status.FAILURE));
        // todo: remove next line
        LOGGER.info("FailureMessage: {}", Base64Util.base64decode(chunkResult.getItems().get(0).getData()));
        assertThat(chunkResult.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
    }

    private ChunkProcessorBean getInitializedBean() {
        return new ChunkProcessorBean();
    }

    private Flow getFlow(String invocationMethod, JavaScript javascript) throws Exception {
        return new FlowBuilder()
                .setContent(getFlowContent(invocationMethod, javascript))
                .build();
    }

    private FlowContent getFlowContent(String invocationMethod, JavaScript javascript) throws Exception {
        return new FlowContentBuilder()
                .setComponents(Arrays.asList(getFlowComponent(invocationMethod, javascript)))
                .build();
    }

    private FlowComponent getFlowComponent(String invocationMethod, JavaScript javascript) throws Exception {
        return new FlowComponentBuilder()
                .setContent(getFlowComponentContent(invocationMethod, javascript))
                .build();
    }

    private FlowComponentContent getFlowComponentContent(String invocationMethod, JavaScript javascript) throws Exception {
        return new FlowComponentContentBuilder()
                .setInvocationMethod(invocationMethod)
                .setJavascripts(Arrays.asList(
                                javascript,
                                JsonUtil.fromJson(resourceToString(modulesInfoModuleResource), JavaScript.class, MixIns.getMixIns()),
                                JsonUtil.fromJson(resourceToString(useModuleResource), JavaScript.class, MixIns.getMixIns())))
                .build();
    }

    private JavaScript getJavaScript(String javascript) {
        return new JavaScriptBuilder()
                .setJavascript(Base64Util.base64encode(javascript))
                .build();
    }

    private String getJavaScriptToUpperFunction() {
        return ""
                + "function " + javaScriptUppercaseInvocationMethod + "(str) {\n"
                + "    return str.toUpperCase();\n"
                + "}\n";
    }

    private String getJavaScriptWhichThrowsException() {
        return ""
                + "function " + javaScriptThrowExceptionInvocationMethod + "(str) {\n"
                + "    if(str === \"throw\") {\n"
                + "      throw \"this is an exception from JavaScript\";\n"
                + "    } else {\n"
                + "      return str;\n"
                + "    }\n"
                + "}\n";
    }

    private String getJavaScriptConcatenateProcessDataFunction() {
        return ""
                + "function " + javaScriptConcatenateInvocationMethod + "(str, processData) {\n"
                + "    return processData.submitter + str + processData.format;\n"
                + "}\n";
    }

    private String resourceToString(String resourceName) throws Exception {
        final java.net.URL url = ChunkProcessorBeanTest.class.getResource(resourceName);
        final java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
        return new String(java.nio.file.Files.readAllBytes(resPath), "UTF8");
    }
}
