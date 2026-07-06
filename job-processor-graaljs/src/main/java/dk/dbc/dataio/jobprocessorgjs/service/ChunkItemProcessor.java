package dk.dbc.dataio.jobprocessorgjs.service;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.graaljs.core.JsInterop;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessorgjs.javascript.GraalJsScript;
import dk.dbc.dataio.jobprocessorgjs.logstore.LogStoreWriter;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.javascript.recordprocessing.IgnoreRecord;
import org.graalvm.polyglot.PolyglotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class ChunkItemProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkItemProcessor.class);

    private final long jobId;
    private final long chunkId;
    private final GraalJsScript script;
    private final String supplementaryData;
    private final LogStoreWriter logStoreWriter;

    public ChunkItemProcessor(long jobId, long chunkId, GraalJsScript script, String supplementaryData,
                              LogStoreWriter logStoreWriter) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.script = script;
        this.supplementaryData = supplementaryData;
        this.logStoreWriter = logStoreWriter;
    }

    public ChunkItem process(ChunkItem chunkItem) {
        if (chunkItem.getStatus() != ChunkItem.Status.SUCCESS) {
            return ChunkItem.ignoredChunkItem()
                    .withId(chunkItem.getId())
                    .withData(String.format("Ignored by job-processor since returned status was {%s}",
                            chunkItem.getStatus()))
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId());
        }

        // Discard any JS log events left on this (reused) consumer thread before processing
        // the item, so getEvents() below returns only the events produced for this item.
        JsInterop.logCollector.clearEvents();
        try {
            String itemData;
            Object supplement;

            if (chunkItem.isTyped() && chunkItem.getType().getFirst() == ChunkItem.Type.ADDI) {
                AddiRecord addiRecord = new AddiReader(
                        new ByteArrayInputStream(chunkItem.getData())).next();
                itemData = StringUtil.asString(addiRecord.getContentData(), chunkItem.getEncoding());
                supplement = script.eval("(" + StringUtil.asString(addiRecord.getMetaData()) + ")");
            } else {
                itemData = StringUtil.asString(chunkItem.getData(), chunkItem.getEncoding());
                supplement = script.eval("(" + supplementaryData + ")");
            }

            LOGGER.info("process(): invoking '{}' in script '{}' for item {}/{}/{}",
                    script.getInvocationMethod(), script.getScriptId(),
                    jobId, chunkId, chunkItem.getId());

            String scriptResult = script.invoke(new Object[]{itemData, supplement});

            if (scriptResult.isEmpty()) {
                return ChunkItem.ignoredChunkItem()
                        .withId(chunkItem.getId())
                        .withData("Ignored by job-processor since returned data was empty")
                        .withType(ChunkItem.Type.STRING)
                        .withTrackingId(chunkItem.getTrackingId());
            }
            return ChunkItem.successfulChunkItem()
                    .withId(chunkItem.getId())
                    .withData(scriptResult)
                    .withType(ChunkItem.Type.UNKNOWN)
                    .withTrackingId(chunkItem.getTrackingId());

        } catch (PolyglotException e) {
            if (e.isHostException()) {
                Throwable hostException = e.asHostException();
                if (hostException instanceof IgnoreRecord) {
                    return ChunkItem.ignoredChunkItem()
                            .withId(chunkItem.getId())
                            .withData(hostException.getMessage())
                            .withType(ChunkItem.Type.STRING)
                            .withTrackingId(chunkItem.getTrackingId());
                }
                if (hostException instanceof FailRecord) {
                    Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.ERROR, hostException.getMessage());
                    diagnostic.withTag(FailRecord.class.getName());
                    return ChunkItem.failedChunkItem()
                            .withId(chunkItem.getId())
                            .withData(hostException.getMessage())
                            .withType(ChunkItem.Type.STRING)
                            .withTrackingId(chunkItem.getTrackingId())
                            .withDiagnostics(diagnostic);
                }
            }
            return failedItem(chunkItem, e);
        } catch (Throwable t) {
            LOGGER.error("process(): unhandled exception for item {}/{}/{}", jobId, chunkId, chunkItem.getId(), t);
            return failedItem(chunkItem, t);
        } finally {
            // Persist whatever the JavaScript logged for this item, regardless of outcome,
            // then clear the thread-local collector for the next item.
            logStoreWriter.write(String.valueOf(jobId), chunkId, chunkItem.getId(),
                    JsInterop.logCollector.getEvents());
            JsInterop.logCollector.clearEvents();
        }
    }

    private ChunkItem failedItem(ChunkItem chunkItem, Throwable t) {
        String stackTrace = StringUtil.getStackTraceString(t, "");
        Diagnostic.Level level = stackTrace.contains("Illegal operation on control field")
                ? Diagnostic.Level.ERROR
                : Diagnostic.Level.FATAL;
        Diagnostic diagnostic = new Diagnostic(level, "Exception caught during javascript processing",
                stackTrace, null, null);
        return ChunkItem.failedChunkItem()
                .withId(chunkItem.getId())
                .withData(stackTrace)
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(chunkItem.getTrackingId())
                .withDiagnostics(diagnostic);
    }
}
