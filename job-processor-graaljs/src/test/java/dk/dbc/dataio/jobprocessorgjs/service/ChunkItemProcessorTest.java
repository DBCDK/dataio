package dk.dbc.dataio.jobprocessorgjs.service;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobprocessorgjs.javascript.GraalJsScript;
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ChunkItemProcessorTest {
    private static final String SCRIPT_ID = "main.js";
    private static final String FUNCTION = "process";
    private static final String ADDITIONAL_ARGS = "{}";

    private static Engine engine;

    @BeforeAll
    static void createEngine() {
        engine = Engine.newBuilder("js").build();
    }

    @AfterAll
    static void closeEngine() {
        engine.close();
    }

    @Test
    void process_itemWithFailedStatus_returnsIgnoredItem() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setStatus(ChunkItem.Status.FAILURE).setData("data").setTrackingId("t1")
                .build();
        try (GraalJsScript script = identityScript()) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    void process_itemWithIgnoredStatus_returnsIgnoredItem() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setStatus(ChunkItem.Status.IGNORE).setData("data").setTrackingId("t1")
                .build();
        try (GraalJsScript script = identityScript()) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    void process_successItem_scriptReturnsData_returnsSuccessItem() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        try (GraalJsScript script = scriptWith("export function process(d, s) { return d.toUpperCase(); }")) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.SUCCESS));
            assertThat(new String(result.getData(), StandardCharsets.UTF_8), is("HELLO"));
        }
    }

    @Test
    void process_successItem_scriptReturnsNull_returnsIgnoredItem() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        try (GraalJsScript script = scriptWith("export function process(d, s) { return null; }")) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    void process_successItem_scriptReturnsEmptyString_returnsIgnoredItem() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        try (GraalJsScript script = scriptWith("export function process(d, s) { return ''; }")) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    void process_successItem_scriptThrowsIgnoreRecord_returnsIgnoredItem() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        String js = "export function process(d, s) {" +
                " Java.type('dk.dbc.javascript.recordprocessing.IgnoreRecord').doThrow('skip'); }";
        try (GraalJsScript script = scriptWith(js)) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.IGNORE));
        }
    }

    @Test
    void process_successItem_scriptThrowsFailRecord_returnsFailedItemWithErrorDiagnostic() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        String js = "export function process(d, s) {" +
                " Java.type('dk.dbc.javascript.recordprocessing.FailRecord').doThrow('bad record'); }";
        try (GraalJsScript script = scriptWith(js)) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
            assertThat(result.getDiagnostics().getFirst().getLevel(), is(Diagnostic.Level.ERROR));
        }
    }

    @Test
    void process_successItem_scriptThrowsGenericException_returnsFailedItemWithFatalDiagnostic() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        try (GraalJsScript script = scriptWith("export function process(d, s) { throw 'something went wrong'; }")) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
            assertThat(result.getDiagnostics().getFirst().getLevel(), is(Diagnostic.Level.FATAL));
        }
    }

    @Test
    void process_successItem_scriptThrowsIllegalOperationOnControlField_returnsFailedItemWithErrorDiagnostic() throws IOException {
        ChunkItem input = new ChunkItemBuilder()
                .setId(1).setData("hello").setTrackingId("t1")
                .build();
        try (GraalJsScript script = scriptWith(
                "export function process(d, s) { throw 'Illegal operation on control field'; }")) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.FAILURE));
            assertThat(result.getDiagnostics().getFirst().getLevel(), is(Diagnostic.Level.ERROR));
        }
    }

    @Test
    void process_addiTypeItem_contentPassedToScriptAndMetaUsedAsSupplement() throws IOException {
        byte[] meta = "{\"format\":\"test-format\"}".getBytes(StandardCharsets.UTF_8);
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        ChunkItem input = ChunkItem.successfulChunkItem()
                .withId(1)
                .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                .withData(new AddiRecord(meta, content).getBytes())
                .withTrackingId("t1");

        String js = "export function process(data, supplement) { return supplement.format + ':' + data; }";
        try (GraalJsScript script = scriptWith(js)) {
            ChunkItem result = processor(script).process(input);
            assertThat(result.getStatus(), is(ChunkItem.Status.SUCCESS));
            assertThat(new String(result.getData(), StandardCharsets.UTF_8), is("test-format:hello"));
        }
    }

    private static ChunkItemProcessor processor(GraalJsScript script) {
        return new ChunkItemProcessor(1, 0, script, ADDITIONAL_ARGS);
    }

    private static GraalJsScript identityScript() throws IOException {
        return scriptWith("export function process(d, s) { return d; }");
    }

    private static GraalJsScript scriptWith(String jsSource) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(SCRIPT_ID));
            zos.write(jsSource.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return new GraalJsScript(SCRIPT_ID, FUNCTION, baos.toByteArray(), engine);
    }
}
