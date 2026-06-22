package dk.dbc.dataio.jobprocessorgjs.service;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessorgjs.javascript.GraalJsScript;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.javascript.recordprocessing.IgnoreRecord;
import org.graalvm.polyglot.PolyglotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;

public class ChunkItemProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkItemProcessor.class);

    private final long jobId;
    private final long chunkId;
    private final GraalJsScript script;
    private final String supplementaryData;

    public ChunkItemProcessor(long jobId, long chunkId, GraalJsScript script, String supplementaryData) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.script = script;
        this.supplementaryData = supplementaryData;
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

        mdcPut(chunkItem);
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

            if (scriptResult == null || scriptResult.isEmpty()) {
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
            mdcRemove(chunkItem);
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

    private void mdcPut(ChunkItem chunkItem) {
        MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY,
                LogStoreTrackingId.create(String.valueOf(jobId), chunkId, chunkItem.getId()).toString());
    }

    private void mdcRemove(ChunkItem chunkItem) {
        MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY, "true");
        LOGGER.info("log-store commit for item {}/{}/{}", jobId, chunkId, chunkItem.getId());
        MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY);
        MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
    }
}
