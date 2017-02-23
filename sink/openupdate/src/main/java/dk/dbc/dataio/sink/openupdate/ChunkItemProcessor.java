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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;

import javax.xml.ws.WebServiceException;
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
    private final ChunkItem chunkItem;

    private int addiRecordIndex;
    private int totalNumberOfAddiRecords;
    private List<Diagnostic> diagnostics;
    private StringBuilder crossAddiRecordsMessage;

    // sleep time for webservice call retries
    // set as global to be able to override in tests
    int sleepDuration = 3000;

    /**
     * @param chunkItem chunk item to be processed
     * @param addiRecordPreprocessor ADDI record pre-processor
     * @param openUpdateServiceConnector OpenUpdate webservice connector
     * @param updateRecordResultMarshaller updateRecordResultMarshaller
     * @throws NullPointerException if given null-valued argument
     */
    public ChunkItemProcessor(ChunkItem chunkItem, AddiRecordPreprocessor addiRecordPreprocessor,
                              OpenUpdateServiceConnector openUpdateServiceConnector,
                              UpdateRecordResultMarshaller updateRecordResultMarshaller) throws NullPointerException {
        this.chunkItem = InvariantUtil.checkNotNullOrThrow(chunkItem, "chunkItem");
        this.addiRecordPreprocessor = InvariantUtil.checkNotNullOrThrow(addiRecordPreprocessor, "addiRecordPreprocessor");
        this.openUpdateServiceConnector = InvariantUtil.checkNotNullOrThrow(openUpdateServiceConnector, "openUpdateServiceConnector");
        this.updateRecordResultMarshaller = InvariantUtil.checkNotNullOrThrow(updateRecordResultMarshaller, "updateRecordResultMarshaller");
    }

    /**
     * Calls the update web service for all ADDI records contained in this chunk item and combines
     * the results to into a single result chunk item.
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
            return ObjectFactory.buildFailedChunkItem(
                    chunkItem.getId(),
                    "Failed when reading Addi records for processed ChunkItem: " + chunkItem.getId() + " -> " + StringUtil.getStackTraceString(t),
                    ChunkItem.Type.STRING,
                    chunkItem.getTrackingId());
        }

        final Optional<AddiStatus> failed = addiRecordsForItem.stream()
                // retrieve the AddiStatus from each call to OpenUpdate
                .map(addiRecord -> callUpdateService(addiRecord, addiRecordsForItem.indexOf(addiRecord), queueProvider))
                // only collect the failed status'
                .filter(addiStatus -> addiStatus == AddiStatus.FAILED_STACKTRACE || addiStatus == AddiStatus.FAILED_VALIDATION)
                // retrieve the first -> if a failed status exist the Optional object has a present object associated with it
                .findFirst();

        final ChunkItem result = ObjectFactory.buildSuccessfulChunkItem(chunkItem.getId(),
                getItemContentCrossAddiRecords(), ChunkItem.Type.STRING, chunkItem.getTrackingId());

        if(failed.isPresent()) {
            diagnostics.stream().forEach(result::appendDiagnostics);
        }
        return result;
    }

    private AddiStatus handleDiagnosticsForError(Throwable t){
        crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_STACKTRACE));
        crossAddiRecordsMessage.append(StringUtil.getStackTraceString(t));
        diagnostics.add(buildDiagnosticForGenericUpdateRecordError(t));
        return AddiStatus.FAILED_STACKTRACE;
    }

    private AddiStatus callUpdateService(AddiRecord addiRecord, int addiRecordIndex, String queueProvider) {
        return callUpdateService(addiRecord, addiRecordIndex, queueProvider, 0);
    }

    private AddiStatus callUpdateService(AddiRecord addiRecord, int addiRecordIndex, String queueProvider, int currentRetry) {
        this.addiRecordIndex = addiRecordIndex + 1;
        try {
            final AddiRecordPreprocessor.Result preprocessorResult = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);

            final UpdateRecordResult webserviceResult = openUpdateServiceConnector.updateRecord(
                    preprocessorResult.getSubmitter(),
                    preprocessorResult.getTemplate(),
                    preprocessorResult.getBibliographicRecord(),
                    chunkItem.getTrackingId());

            if (webserviceResult.getUpdateStatus() == UpdateStatusEnum.OK) {
                crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.OK));
                return AddiStatus.OK;
            }

            crossAddiRecordsMessage.append(getAddiRecordMessage(AddiStatus.FAILED_VALIDATION));
            crossAddiRecordsMessage.append(updateRecordResultMarshaller.asXml(webserviceResult));

            final UpdateRecordErrorInterpreter updateRecordErrorInterpreter = new UpdateRecordErrorInterpreter();
            diagnostics.addAll(updateRecordErrorInterpreter.getDiagnostics(webserviceResult, addiRecord));

            return AddiStatus.FAILED_VALIDATION;

        } catch(WebServiceException e) {
            int retries = 6;
            // http error codes:
            final int[] errorCodes = {404, 502, 503};
            if (IntStream.of(errorCodes).anyMatch(n -> n == getStatusCodeFromError(e)) && currentRetry < retries) {
                try {
                    Thread.sleep((currentRetry + 1) * sleepDuration);
                    return callUpdateService(addiRecord, addiRecordIndex, queueProvider, ++currentRetry);
                } catch(InterruptedException e2) {
                    return callUpdateService(addiRecord, addiRecordIndex, queueProvider, ++currentRetry);
                }
            } else {
                return handleDiagnosticsForError(e);
            }
        } catch (Throwable t) {
            return handleDiagnosticsForError(t);
        }
    }

    private int getStatusCodeFromError(WebServiceException e) {
        if(e.getMessage() == null) return -1;
        // there isn't a method to get the error code like in HTTPException
        Pattern p = Pattern.compile("HTTP status code (\\d+)");
        Matcher m = p.matcher(e.getMessage());
        if(m.find()) {
            return Integer.valueOf(m.group(1));
        }
        return -1;
    }

    private Diagnostic buildDiagnosticForGenericUpdateRecordError(Throwable t) {
        final String diagnosticMessage = t.getMessage() != null ? t.getMessage() : t.getClass().getCanonicalName()
                + " occurred while calling openUpdateService";
        return ObjectFactory.buildFatalDiagnostic(diagnosticMessage, t);
    }

    private String getAddiRecordMessage(AddiStatus addiStatus) {
        final String itemResultTemplate = "Addi record with trackingID %s : %s out of %s -> %s\n";
        return String.format(itemResultTemplate, chunkItem.getTrackingId(), addiRecordIndex, totalNumberOfAddiRecords, addiStatus);
    }

    private String getItemContentCrossAddiRecords() {
        if(!diagnostics.isEmpty()) {
            appendDiagnosticsContentToCrossAddiRecordsMessage();
        }
        return crossAddiRecordsMessage.toString();
    }

    private void appendDiagnosticsContentToCrossAddiRecordsMessage() {
        crossAddiRecordsMessage.append(System.lineSeparator());
        for(Diagnostic diagnostic : diagnostics) {
            if(diagnostic.getStacktrace() != null) {
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
