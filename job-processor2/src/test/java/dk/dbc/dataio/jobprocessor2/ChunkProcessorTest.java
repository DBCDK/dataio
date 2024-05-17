package dk.dbc.dataio.jobprocessor2;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jse.artemis.common.service.HealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ChunkProcessorTest {

    /* To update the 'simple.jsar' archive, simply change directory into the
       'src/test/resources/jsar' directory, edit the necessary files in the
       'files' subdirectory, and run 'zip -j simple.jsar files/*' */
    private static byte[] SIMPLE_JSAR = getJsarBytes(Path.of("src", "test", "resources", "jsar", "simple.jsar"));
    private static String ENTRYPOINT_SCRIPT = "entrypoint.js";

    private static final String FUNCTION_CONCAT = "concat";
    private static final String FUNCTION_TO_EMPTY_STRING = "toEmptyString";
    private static final String FUNCTION_TO_UPPER_CASE = "toUpperCase";
    private static final String FUNCTION_THROW_EXCEPTION = "throwException";

    private static final int JOB_ID = 42;
    private static final long SUBMITTER_ID = 123;
    private static final String TRACKING_ID = "trackingId_";
    private static final String FORMAT = "aFormat";
    private static final String ADDITIONAL_ARGS = String.format("{\"format\":\"%s\",\"submitter\":%s}", FORMAT, SUBMITTER_ID);

    @BeforeEach
    public void initCache() {
        ChunkProcessor.clearFlowCache();
    }

    @Test
    void callScript() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_TO_UPPER_CASE, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("test"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 1);
        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] data", getString(chunkItem.getData()),
                is("TEST"));
    }

    @Test
    void emptyInputChunk_emptyOutputChunk() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_TO_UPPER_CASE, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(Collections.emptyList())
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);
        
        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 0);
    }

    @Test
    void exceptionThrownFromScript_failedOutputChunkItem() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_THROW_EXCEPTION, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("throw"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostics", chunkItem.getDiagnostics().size(),
                is(1));
        assertThat("Chunk item[0] diagnostic level", chunkItem.getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.FATAL));
        assertThat("Chunk item[0] diagnostic stacktrace", chunkItem.getDiagnostics().get(0).getStacktrace(),
                is(notNullValue()));
    }

    @Test
    void illegalOperationOnControlFieldException_failedOutputChunkItem() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_THROW_EXCEPTION, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("illegal-operation-on-control-field"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[0] diagnostics", chunkItem.getDiagnostics().size(),
                is(1));
        assertThat("Chunk item[0] diagnostic level", chunkItem.getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.ERROR));
        assertThat("Chunk item[0] diagnostic stacktrace", chunkItem.getDiagnostics().get(0).getStacktrace(),
                is(notNullValue()));
    }

    @Test
    void exceptionThrownFromOneOutOfThree_failedOutputChunkItem() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_THROW_EXCEPTION, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("ok1", "throw", "ok3"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 3);

        Iterator<ChunkItem> iterator = outputChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem chunkItem0 = iterator.next();
        assertThat("Chunk item[0] status", chunkItem0.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] data", getString(chunkItem0.getData()),
                is("ok1"));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        ChunkItem chunkItem1 = iterator.next();
        assertThat("Chunk item[1] status", chunkItem1.getStatus(),
                is(ChunkItem.Status.FAILURE));
        assertThat("Chunk item[1] diagnostic", chunkItem1.getDiagnostics().size(),
                is(1));
        assertThat("Chunk item[1] diagnostic stacktrace", chunkItem1.getDiagnostics().get(0).getStacktrace(),
                is(notNullValue()));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        ChunkItem chunkItem2 = iterator.next();
        assertThat("Chunk item[2] status", chunkItem2.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[2] data", getString(chunkItem2.getData()),
                is("ok3"));
    }

    @Test
    void emptyResultFromScript_ignoredOutputChunkItem() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_TO_EMPTY_STRING, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("test"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 1);

        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.IGNORE));
    }

    @Test
    void ignoredByScript_ignoredOutputChunkItem() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_THROW_EXCEPTION, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("ignore"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 1);

        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.IGNORE));
    }

    @Test
    void failedByScript_failedOutputChunkItem() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_THROW_EXCEPTION, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("fail"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 1);

        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.FAILURE));
    }

    @Test
    void skipsInputChunkItemsWithStatusIgnoreAndFailure() {
        List<ChunkItem> items = new ArrayList<>();
        items.add(new ChunkItemBuilder().setId(0).setData("test").build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.IGNORE).build());

        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_TO_UPPER_CASE, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(items)
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 3);

        Iterator<ChunkItem> iterator = outputChunk.iterator();
        assertThat("Chunk has item[0]", iterator.hasNext(), is(true));
        ChunkItem chunkItem0 = iterator.next();
        assertThat("Chunk item[0] status", chunkItem0.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] data", getString(chunkItem0.getData()),
                is("TEST"));

        assertThat("Chunk has item[1]", iterator.hasNext(), is(true));
        ChunkItem chunkItem1 = iterator.next();
        assertThat("Chunk item[1] status", chunkItem1.getStatus(),
                is(ChunkItem.Status.IGNORE));

        assertThat("Chunk has item[2]", iterator.hasNext(), is(true));
        ChunkItem chunkItem2 = iterator.next();
        assertThat("Chunk item[2] status", chunkItem2.getStatus(),
                is(ChunkItem.Status.IGNORE));
    }

    @Test
    void ScriptCalledWithAdditionalArgs() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_CONCAT, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("test"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 1);
        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] data", getString(chunkItem.getData()),
                is(SUBMITTER_ID + " test " + FORMAT));
    }

    @Test
    void chunkItemIsOfTypeAddi_scriptArgumentsAreReadFromAddiContentAndAddiMetadata() throws JSONBException {
        AddiMetaData addiMetaData = new AddiMetaData()
                .withSubmitterNumber((int) SUBMITTER_ID)
                .withFormat(FORMAT);
        AddiRecord addiRecord = new AddiRecord(
                new JSONBContext().marshall(addiMetaData).getBytes(StandardCharsets.UTF_8),
                "test".getBytes(StandardCharsets.UTF_8));

        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(Collections.singletonList(
                        ChunkItem.successfulChunkItem()
                                .withId(0)
                                .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                                .withData(addiRecord.getBytes())))
                .build();

        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_CONCAT, SIMPLE_JSAR);

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        Chunk outputChunk = chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), "{}");

        assertChunk(outputChunk, JOB_ID, inputChunk.getChunkId(), 1);
        ChunkItem chunkItem = outputChunk.getItems().get(0);
        assertThat("Chunk item[0] status", chunkItem.getStatus(),
                is(ChunkItem.Status.SUCCESS));
        assertThat("Chunk item[0] data", getString(chunkItem.getData()),
                is(SUBMITTER_ID + " test " + FORMAT));
    }

    @Test
    void cachesFlow() {
        Flow flow = getFlow(ENTRYPOINT_SCRIPT, FUNCTION_TO_UPPER_CASE, SIMPLE_JSAR);
        Chunk inputChunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(JOB_ID)
                .setItems(getChunkItems("test"))
                .build();

        ChunkProcessor chunkProcessor = getChunkProcessor(flow);
        chunkProcessor.process(inputChunk, flow.getId(), flow.getVersion(), ADDITIONAL_ARGS);
        assertThat("flow cached", chunkProcessor.getCacheView().values().stream().anyMatch(f -> f.flow.equals(flow)),
                is(true));
    }

    private static ChunkProcessor getChunkProcessor(Flow flow) {
        return new ChunkProcessor(mock(HealthService.class), id -> flow);
    }

    private static void assertChunk(Chunk chunk, int jobID, long chunkId, int chunkSize) {
        assertThat("Chunk", chunk, is(notNullValue()));
        assertThat("Chunk job ID", chunk.getJobId(), is(jobID));
        assertThat("Chunk ID", chunk.getChunkId(), is(chunkId));
        assertThat("Chunk size", chunk.size(), is(chunkSize));
        assertThat("Chunk type", chunk.getType(), is(Chunk.Type.PROCESSED));
    }

    private static List<ChunkItem> getChunkItems(String... data) {
        List<ChunkItem> items = new ArrayList<>(data.length);
        int chunkId = 0;
        for (String itemData : data) {
            items.add(new ChunkItemBuilder()
                    .setId(chunkId++)
                    .setData(StringUtil.asBytes(itemData))
                    .setTrackingId(TRACKING_ID + chunkId)
                    .build());
        }
        return items;
    }

    private static Flow getFlow(String entrypointScript, String entrypointFunction, byte[] jsar) {
        return new FlowBuilder()
                .setContent(new FlowContentBuilder()
                        .setEntrypointScript(entrypointScript)
                        .setEntrypointFunction(entrypointFunction)
                        .setJsar(jsar)
                        .build())
                .build();
    }

    private static byte[] getJsarBytes(Path jsar) {
        try {
            return Files.readAllBytes(jsar);
        } catch (IOException e) {
            throw new UncheckedIOException(e) ;
        }
    }

    private static String getString(byte[] bytes, Charset charset) {
        return new String(bytes, charset);
    }

    private static String getString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
