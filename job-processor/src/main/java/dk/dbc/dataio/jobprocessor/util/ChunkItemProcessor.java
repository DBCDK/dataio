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

package dk.dbc.dataio.jobprocessor.util;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor.javascript.Script;
import dk.dbc.dataio.logstore.types.LogStoreTrackingId;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.javascript.recordprocessing.IgnoreRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.util.List;

public class ChunkItemProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkItemProcessor.class);
    private final long jobId;
    private final long chunkId;
    private final List<Script> scripts;
    private final String supplementaryData;

    public ChunkItemProcessor(long jobId, long chunkId, List<Script> scripts, String supplementaryData) {
        this.jobId = jobId;
        this.chunkId = chunkId;
        this.scripts = scripts;
        this.supplementaryData = supplementaryData;
    }

    public ChunkItem process(ChunkItem chunkItem) {
        try {
            if (chunkItem.getStatus() != ChunkItem.Status.SUCCESS) {
                return ChunkItem.ignoredChunkItem()
                        .withId(chunkItem.getId())
                        .withData(String.format("Ignored by job-processor since returned status was {%s}", chunkItem.getStatus()))
                        .withType(ChunkItem.Type.STRING)
                        .withTrackingId(chunkItem.getTrackingId());
            }

            final String logstoreTrackingId = logstoreMdcPut(chunkItem);

            final ScriptArguments arguments = new ScriptArguments(scripts.get(0), chunkItem, supplementaryData);
            String scriptResult = arguments.getItemData();
            for (Script script : scripts) {
                scriptResult = invokeScript(script, scriptResult, arguments.getSupplement(), logstoreTrackingId);
                if (scriptResult.isEmpty()) {
                    // terminate pipeline processing
                    break;
                }
            }
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
        } catch (IgnoreRecord e) {
            return ChunkItem.ignoredChunkItem()
                    .withId(chunkItem.getId())
                    .withData(e.getMessage())
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId());
        } catch (FailRecord e) {
            final Diagnostic diagnostic = new Diagnostic(
                Diagnostic.Level.ERROR, e.getMessage());
            diagnostic.withTag(e.getClass().getName());
            return ChunkItem.failedChunkItem()
                    .withId(chunkItem.getId())
                    .withData(e.getMessage())
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId())
                    .withDiagnostics(diagnostic);
        } catch (Throwable t) {
            LOGGER.error("process(): unhandled exception caught during javascript processing", t);
            return createChunkItemForUnhandledJavascriptException(chunkItem, t);
        } finally {
            logstoreMdcRemove(chunkItem);
        }
    }

    private ChunkItem createChunkItemForUnhandledJavascriptException(ChunkItem chunkItem, Throwable t) {
        final String stackTraceString = StringUtil.getStackTraceString(t, "");
        Diagnostic.Level diagLevel = Diagnostic.Level.FATAL;
        if (stackTraceString.contains("Illegal operation on control field")) {
            // This exception happens due to the ISO2709
            // reader erroneously creating control fields
            // from empty data fields, and therefore should
            // not be flagged as a FATAL error on the
            // processing side.
            diagLevel = Diagnostic.Level.ERROR;
        }
        final Diagnostic diagnostic = new Diagnostic(diagLevel,
                "Exception caught during javascript processing",
                stackTraceString, null, null);
        return ChunkItem.failedChunkItem()
                    .withId(chunkItem.getId())
                    .withData(diagnostic.getStacktrace())
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId())
                    .withDiagnostics(diagnostic);
    }

    private String logstoreMdcPut(ChunkItem chunkItem) {
        final String logstoreTrackingId = LogStoreTrackingId.create(
                String.valueOf(jobId), chunkId, chunkItem.getId()).toString();
        MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY, logstoreTrackingId);
        return logstoreTrackingId;
    }

    private void logstoreMdcRemove(ChunkItem chunkItem) {
        MDC.put(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY, "true");
        // This timing assumes the use of LogStoreMergingJdbcAppender to be meaningful
        final StopWatch timer = new StopWatch();
        LOGGER.info("Done");
        MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_COMMIT_MDC_KEY);
        MDC.remove(LogStoreTrackingId.LOG_STORE_TRACKING_ID_MDC_KEY);
        LOGGER.info("logstoreMdcRemove(): log-store batch insert for item {}/{}/{} took {} milliseconds",
                jobId, chunkId, chunkItem.getId(), timer.getElapsedTime());
    }

    private String invokeScript(Script script, String data, Object supplementaryDataObject, String trackingId) throws Throwable {
        LOGGER.info("invokeScript(): starting javascript [{}] with invocation method: [{}] and logging ID [{}]",
                script.getScriptId(), script.getInvocationMethod(), trackingId);
        final Object result = script.invoke(new Object[]{data, supplementaryDataObject});
        return (String) result;
    }

    private static class ScriptArguments {
        private final String itemData;
        private final Object supplement;

        ScriptArguments(Script script, ChunkItem chunkItem, String nonAddiTypeSupplement) throws Throwable {
            if (chunkItem.isTyped() && chunkItem.getType().get(0) == ChunkItem.Type.ADDI) {
                final AddiRecord addiRecord = new AddiReader(new ByteArrayInputStream(chunkItem.getData())).next();
                itemData = StringUtil.asString(addiRecord.getContentData(), chunkItem.getEncoding());
                supplement = evalSupplement(script, StringUtil.asString(addiRecord.getMetaData()));
            } else {
                itemData = StringUtil.asString(chunkItem.getData(), chunkItem.getEncoding());
                supplement = evalSupplement(script, nonAddiTypeSupplement);
            }
        }

        String getItemData() {
            return itemData;
        }

        Object getSupplement() {
            return supplement;
        }

        private Object evalSupplement(Script script, String supp) throws Throwable {
            // Something about why you need parentheses in the string around the json
            // when trying to evaluate the json in javascript (rhino):
            // https://rayfd.wordpress.com/2007/03/28/why-wont-eval-eval-my-json-or-json-object-object-literal/
            return script.eval("(" + supp + ")"); // notice the parentheses!
        }
    }
}
