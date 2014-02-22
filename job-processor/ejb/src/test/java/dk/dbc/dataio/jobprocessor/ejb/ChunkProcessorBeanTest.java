package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64decode;
import static dk.dbc.dataio.jobprocessor.util.Base64Util.base64encode;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChunkProcessorBeanTest {
    private final long  jobId = 42;
    private final String javaScriptUppercaseInvocationMethod = "toUpper";
    private final String javaScriptConcatenateInvocationMethod = "doConcatenation";
    private final String modulesInfoModuleResource = "/ModulesInfo.json";
    private final String useModuleResource = "/Use.json";

    @Test
    public void process_emptyChunk_returnsEmptyResult() {
        final Chunk emptyChunk = new ChunkBuilder()
                .setJobId(jobId)
                .setRecords(new ArrayList<String>(0))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(emptyChunk);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(emptyChunk.getChunkId()));
        assertThat(chunkResult.getResults().size(), is(0));
    }

    @Test
    public void process_chunkWithData_returnsResultOfJavaScriptProcessing() throws Exception {
        final String record1 = "one";
        final String record2 = "two";
        final Chunk chunk = new ChunkBuilder()
                .setJobId(jobId)
                .setFlow(getFlow(javaScriptUppercaseInvocationMethod, getJavaScript(getJavaScriptToUpperFunction())))
                .setRecords(Arrays.asList(base64encode(record1), base64encode(record2)))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getResults().size(), is(2));
        assertThat(base64decode(chunkResult.getResults().get(0)), is(record1.toUpperCase()));
        assertThat(base64decode(chunkResult.getResults().get(1)), is(record2.toUpperCase()));
    }

    @Test
    public void process_chunkWithDataAndProcessData_returnsResultOfJavaScriptProcessing() throws Exception {
        final String record1 = "one";
        final String record2 = "two";
        final long submitter = 456456L;
        final String format = "DasFormat";
        final Chunk chunk = new ChunkBuilder()
                .setJobId(jobId)
                .setFlow(getFlow(javaScriptConcatenateInvocationMethod, getJavaScript(getJavaScriptConcatenateProcessDataFunction())))
                .setSupplementaryProcessData(new SupplementaryProcessData(submitter, format))
                .setRecords(Arrays.asList(base64encode(record1), base64encode(record2)))
                .build();

        final ChunkProcessorBean chunkProcessorBean = getInitializedBean();
        final ChunkResult chunkResult = chunkProcessorBean.process(chunk);
        assertThat(chunkResult.getJobId(), is(jobId));
        assertThat(chunkResult.getChunkId(), is(chunk.getChunkId()));
        assertThat(chunkResult.getResults().size(), is(2));
        assertThat(base64decode(chunkResult.getResults().get(0)), is("456456oneDasFormat"));
        assertThat(base64decode(chunkResult.getResults().get(1)), is("456456twoDasFormat"));
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
                .setJavascript(base64encode(javascript))
                .build();
    }

    private String getJavaScriptToUpperFunction() {
        return ""
            + "function " + javaScriptUppercaseInvocationMethod + "(str) {\n"
            + "    return str.toUpperCase();\n"
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
