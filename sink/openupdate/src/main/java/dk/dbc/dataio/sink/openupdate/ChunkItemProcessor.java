package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.openupdate.metrics.SimpleTimerMetrics;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import org.eclipse.microprofile.metrics.Tag;

import javax.xml.ws.WebServiceException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class ChunkItemProcessor {
    // TODO: 3/15/16 This class is a mess and needs to be refactored.

    private enum AddiStatus {OK, FAILED_STACKTRACE, FAILED_VALIDATION}

    private final AddiRecordPreprocessor addiRecordPreprocessor;
    private final OpenUpdateServiceConnector openUpdateServiceConnector;
    private final UpdateRecordResultMarshaller updateRecordResultMarshaller;
    private final UpdateRecordErrorInterpreter updateRecordErrorInterpreter;
    private final ChunkItem chunkItem;

    private final MetricsHandlerBean metricsHandler;

    private int addiRecordIndex;
    private int totalNumberOfAddiRecords;
    private List<Diagnostic> diagnostics;
    private StringBuilder crossAddiRecordsMessage;

    private Pattern errorCodePattern = Pattern.compile("HTTP status code (\\d+)");
    // sleep time for webservice call retries
    // set as global to be able to override in tests
    int retrySleepMillis = 3000;
    int maxNumberOfRetries = 6;

    /**
     * @param chunkItem                    chunk item to be processed
     * @param addiRecordPreprocessor       ADDI record pre-processor
     * @param openUpdateServiceConnector   OpenUpdate webservice connector
     * @param updateRecordResultMarshaller updateRecordResultMarshaller
     * @param updateRecordErrorInterpreter {@link UpdateRecordErrorInterpreter} instance
     * @param metricsHandler               MetricsHandlerBean object
     * @throws NullPointerException if given null-valued argument
     */
    public ChunkItemProcessor(ChunkItem chunkItem, AddiRecordPreprocessor addiRecordPreprocessor,
                              OpenUpdateServiceConnector openUpdateServiceConnector,
                              UpdateRecordResultMarshaller updateRecordResultMarshaller,
                              UpdateRecordErrorInterpreter updateRecordErrorInterpreter,
                              MetricsHandlerBean metricsHandler) throws NullPointerException {
        this.chunkItem = InvariantUtil.checkNotNullOrThrow(chunkItem, "chunkItem");
        this.addiRecordPreprocessor = InvariantUtil.checkNotNullOrThrow(addiRecordPreprocessor, "addiRecordPreprocessor");
        this.openUpdateServiceConnector = InvariantUtil.checkNotNullOrThrow(openUpdateServiceConnector, "openUpdateServiceConnector");
        this.updateRecordResultMarshaller = InvariantUtil.checkNotNullOrThrow(updateRecordResultMarshaller, "updateRecordResultMarshaller");
        this.updateRecordErrorInterpreter = InvariantUtil.checkNotNullOrThrow(updateRecordErrorInterpreter, "updateRecordErrorInterpreter");
        this.metricsHandler = metricsHandler;
    }

    /**
     * Calls the update web service for all ADDI records contained in this chunk item and combines
     * the results to into a single result chunk item.
     *
     * @param queueProvider name of queue provider to be included in request
     * @return resulting chunk item
     */
    public ChunkItem processForQueueProvider(String queueProvider) {
        addiRecordIndex = 1;
        diagnostics = new ArrayList<>();
        crossAddiRecordsMessage = new StringBuilder();

        final List<AddiRecord> addiRecordsForItem;
        try {
            addiRecordsForItem = AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
            totalNumberOfAddiRecords = addiRecordsForItem.size();
        } catch (Throwable t) {
            final String message = "Failed to read Addi record(s) from chunk item: " + t.getMessage();
            return ChunkItem.failedChunkItem()
                    .withId(chunkItem.getId())
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(chunkItem.getTrackingId())
                    .withData(message)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, message, t));
        }

        final Optional<AddiStatus> failed = addiRecordsForItem.stream()
                // retrieve the AddiStatus from each call to OpenUpdate
                .map(addiRecord -> callUpdateService(addiRecord, addiRecordsForItem.indexOf(addiRecord), queueProvider))
                // only collect the failed status'
                .filter(addiStatus -> addiStatus == AddiStatus.FAILED_STACKTRACE || addiStatus == AddiStatus.FAILED_VALIDATION)
                // retrieve the first -> if a failed status exist the Optional object has a present object associated with it
                .findFirst();

        final ChunkItem result = ChunkItem.successfulChunkItem()
                .withId(chunkItem.getId())
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(chunkItem.getTrackingId())
                .withData(getItemContentCrossAddiRecords());
        if (failed.isPresent()) {
            result.appendDiagnostics(diagnostics);
        }
        return result;
    }

    private AddiStatus addDiagnosticsForError(Throwable t) {
        crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_STACKTRACE));
        crossAddiRecordsMessage.append(StringUtil.getStackTraceString(t));
        diagnostics.add(buildFatalDiagnostic(t));
        return AddiStatus.FAILED_STACKTRACE;
    }

    private AddiStatus callUpdateService(AddiRecord addiRecord, int addiRecordIndex, String queueProvider) {
        return callUpdateService(addiRecord, addiRecordIndex, queueProvider, 0);
    }

    private AddiStatus callUpdateService(AddiRecord addiRecord, int addiRecordIndex, String queueProvider, int currentRetry) {
        this.addiRecordIndex = addiRecordIndex + 1;
        try {
            final AddiRecordPreprocessor.Result preprocessorResult = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);

            long updateServiceRequestStartTime = System.currentTimeMillis();

            final UpdateRecordResult webserviceResult = openUpdateServiceConnector.updateRecord(
                    preprocessorResult.getSubmitter(),
                    preprocessorResult.getTemplate(),
                    preprocessorResult.getBibliographicRecord(),
                    chunkItem.getTrackingId());

            metricsHandler.update(SimpleTimerMetrics.UPDATE_SERVICE_REQUESTS,
                    Duration.ofMillis(System.currentTimeMillis() - updateServiceRequestStartTime),
                    new Tag("queueProvider", queueProvider),
                    new Tag("template", preprocessorResult.getTemplate()));

            if (webserviceResult.getUpdateStatus() == UpdateStatusEnum.OK) {
                crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.OK));
                return AddiStatus.OK;
            }

            crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_VALIDATION));
            crossAddiRecordsMessage.append(updateRecordResultMarshaller.asXml(webserviceResult));

            diagnostics.addAll(updateRecordErrorInterpreter.getDiagnostics(webserviceResult, addiRecord));

            return AddiStatus.FAILED_VALIDATION;

        } catch (WebServiceException e) {
            // http error codes:
            final int[] errorCodes = {404, 502, 503};
            if (IntStream.of(errorCodes).anyMatch(n -> n == getStatusCodeFromError(e)) && currentRetry < maxNumberOfRetries) {
                try {
                    Thread.sleep((currentRetry + 1) * retrySleepMillis);
                    return callUpdateService(addiRecord, addiRecordIndex, queueProvider, ++currentRetry);
                } catch (InterruptedException e2) {
                    return callUpdateService(addiRecord, addiRecordIndex, queueProvider, ++currentRetry);
                }
            } else {
                return addDiagnosticsForError(e);
            }
        } catch (Throwable t) {
            return addDiagnosticsForError(t);
        }
    }

    private int getStatusCodeFromError(WebServiceException e) {
        if (e.getMessage() == null) return -1;
        // there isn't a method to get the error code like in HTTPException
        Matcher m = errorCodePattern.matcher(e.getMessage());
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }
        return -1;
    }

    private Diagnostic buildFatalDiagnostic(Throwable t) {
        return new Diagnostic(Diagnostic.Level.FATAL,
                t.getMessage() != null ? t.getMessage() : t.getClass().getCanonicalName()
                        + " occurred while calling openUpdateService", t);
    }

    private String getAddiRecordMessage(AddiStatus addiStatus) {
        final String itemResultTemplate = "Addi record with trackingID %s : %s out of %s -> %s\n";
        return String.format(itemResultTemplate, chunkItem.getTrackingId(), addiRecordIndex, totalNumberOfAddiRecords, addiStatus);
    }

    private String getItemContentCrossAddiRecords() {
        if (!diagnostics.isEmpty()) {
            appendDiagnosticsContentToCrossAddiRecordsMessage();
        }
        return crossAddiRecordsMessage.toString();
    }

    private void appendDiagnosticsContentToCrossAddiRecordsMessage() {
        crossAddiRecordsMessage.append(System.lineSeparator());
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getStacktrace() != null) {
                crossAddiRecordsMessage.append(diagnostic.getMessage());
            } else {
                crossAddiRecordsMessage.append("e01 00 *a").append(diagnostic.getMessage());
                if (diagnostic.getTag() != null) {
                    crossAddiRecordsMessage.append("*b").append(diagnostic.getTag());
                    if (diagnostic.getAttribute() != null) {
                        crossAddiRecordsMessage.append("*c").append(diagnostic.getAttribute());
                    }
                }
            }
            crossAddiRecordsMessage.append(System.lineSeparator());
        }
    }
}
